package com.ford.riva.service;

import com.ford.riva.dto.request.ComparisonRequest;
import com.ford.riva.dto.response.*;
import com.ford.riva.exception.ResourceNotFoundException;
import com.ford.riva.model.Comparison;
import com.ford.riva.model.User;
import com.ford.riva.model.Version;
import com.ford.riva.repository.ComparisonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ComparisonService {

    private final ComparisonRepository comparisonRepository;
    private final UserService userService;
    private final VersionService versionService;

    private static final Set<String> HIGHER_IS_BETTER = Set.of(
            "powerHp", "torqueNm", "tankLiters", "wheelbaseMm", "groundClearanceMm",
            "numAirbags", "approachAngle", "departureAngle", "breakoverAngle",
            "wadingDepth", "cargoCapacityKg", "towingCapacityKg");

    private static final Set<String> LOWER_IS_BETTER = Set.of("price", "acceleration0100");

    @Transactional
    public ComparisonResponse create(ComparisonRequest request) {
        User user = userService.findEntityById(request.userId());
        Set<Version> versions = new HashSet<>();
        for (Integer versionId : request.versionIds()) {
            versions.add(versionService.findEntityById(versionId));
        }
        Comparison comparison = Comparison.builder()
                .user(user).title(request.title()).versions(versions).build();
        return toResponse(comparisonRepository.save(comparison));
    }

    @Transactional
    public ComparisonDetailResponse compareVersions(List<Integer> ids, Long userId) {
        if (ids == null || ids.size() < 2 || ids.size() > 10) {
            throw new IllegalArgumentException("Between 2 and 10 version IDs are required for comparison");
        }
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new IllegalArgumentException("Version IDs must be unique");
        }
        List<Version> versions = ids.stream().map(versionService::findEntityById).toList();
        List<VersionDetailResponse> versionDetails = versions.stream()
                .map(versionService::toDetailResponse).toList();
        Map<String, List<Object>> differences = computeDifferences(versionDetails);
        Map<String, ComparisonDetailResponse.HighlightInfo> highlights = computeHighlights(versionDetails, differences);

        if (userId != null) {
            User user = userService.findEntityById(userId);
            Comparison comparison = Comparison.builder()
                    .user(user)
                    .title(versions.stream().map(Version::getName).reduce((a, b) -> a + " vs " + b).orElse(""))
                    .versions(new HashSet<>(versions)).build();
            Comparison saved = comparisonRepository.save(comparison);
            return new ComparisonDetailResponse(saved.getComparisonId(), userId, saved.getTitle(),
                    saved.getCreatedAt(), versionDetails, differences, highlights);
        }
        return new ComparisonDetailResponse(null, null, null, null, versionDetails, differences, highlights);
    }

    @Transactional(readOnly = true)
    public List<ComparisonResponse> findAll() {
        return comparisonRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ComparisonResponse findById(Integer id) {
        return toResponse(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<ComparisonResponse> findByUserId(Long userId) {
        userService.findEntityById(userId);
        return comparisonRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public void delete(Integer id) {
        comparisonRepository.delete(findEntityById(id));
    }

    private Comparison findEntityById(Integer id) {
        return comparisonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comparison", id));
    }

    private ComparisonResponse toResponse(Comparison c) {
        return new ComparisonResponse(
                c.getComparisonId(), c.getUser().getId(), c.getTitle(), c.getCreatedAt(),
                c.getVersions().stream().map(versionService::toDetailResponse).toList());
    }

    private Map<String, List<Object>> computeDifferences(List<VersionDetailResponse> versions) {
        Map<String, List<Object>> attributes = new LinkedHashMap<>();
        versions.forEach(v -> extractAttributes(attributes, v));
        Map<String, List<Object>> differences = new LinkedHashMap<>();
        attributes.forEach((k, v) -> { if (!allEqual(v)) differences.put(k, v); });
        return differences;
    }

    private void extractAttributes(Map<String, List<Object>> attrs, VersionDetailResponse v) {
        addValue(attrs, "price", v.price()); addValue(attrs, "sunroof", v.sunroof());
        PowertrainResponse p = v.powertrain();
        addValue(attrs, "engine", p != null ? p.engine() : null);
        addValue(attrs, "powerHp", p != null ? p.powerHp() : null);
        addValue(attrs, "torqueNm", p != null ? p.torqueNm() : null);
        addValue(attrs, "transmission", p != null ? p.transmission() : null);
        addValue(attrs, "drivetrain", p != null ? p.drivetrain() : null);
        addValue(attrs, "tankLiters", p != null ? p.tankLiters() : null);
        addValue(attrs, "fuelType", p != null ? p.fuelType() : null);
        DimensionsResponse d = v.dimensions();
        addValue(attrs, "lengthMm", d != null ? d.lengthMm() : null);
        addValue(attrs, "widthMm", d != null ? d.widthMm() : null);
        addValue(attrs, "heightMm", d != null ? d.heightMm() : null);
        addValue(attrs, "wheelbaseMm", d != null ? d.wheelbaseMm() : null);
        addValue(attrs, "groundClearanceMm", d != null ? d.groundClearanceMm() : null);
        SafetyTechResponse s = v.safetyTech();
        addValue(attrs, "numAirbags", s != null ? s.numAirbags() : null);
        addValue(attrs, "abs", s != null ? s.abs() : null);
        addValue(attrs, "stabilityControl", s != null ? s.stabilityControl() : null);
        addValue(attrs, "brakeAssist", s != null ? s.brakeAssist() : null);
        addValue(attrs, "rearCamera", s != null ? s.rearCamera() : null);
        addValue(attrs, "blindSpotMonitor", s != null ? s.blindSpotMonitor() : null);
        SportSpecsResponse sp = v.sportSpecs();
        addValue(attrs, "acceleration0100", sp != null ? sp.acceleration0100() : null);
        addValue(attrs, "convertible", sp != null ? sp.convertible() : null);
        OffroadSpecsResponse o = v.offroadSpecs();
        addValue(attrs, "approachAngle", o != null ? o.approachAngle() : null);
        addValue(attrs, "departureAngle", o != null ? o.departureAngle() : null);
        addValue(attrs, "breakoverAngle", o != null ? o.breakoverAngle() : null);
        addValue(attrs, "wadingDepth", o != null ? o.wadingDepth() : null);
        addValue(attrs, "diffLock", o != null ? o.diffLock() : null);
        addValue(attrs, "hillDescentControl", o != null ? o.hillDescentControl() : null);
        CargoSpecsResponse c = v.cargoSpecs();
        addValue(attrs, "cargoCapacityKg", c != null ? c.cargoCapacityKg() : null);
        addValue(attrs, "towingCapacityKg", c != null ? c.towingCapacityKg() : null);
    }

    private void addValue(Map<String, List<Object>> map, String key, Object value) {
        map.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    }

    private boolean allEqual(List<Object> values) {
        if (values.isEmpty()) return true;
        Object first = values.get(0);
        for (int i = 1; i < values.size(); i++) {
            if (!Objects.equals(first, values.get(i))) return false;
        }
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<String, ComparisonDetailResponse.HighlightInfo> computeHighlights(
            List<VersionDetailResponse> versions, Map<String, List<Object>> differences) {
        Map<String, ComparisonDetailResponse.HighlightInfo> highlights = new LinkedHashMap<>();
        for (var entry : differences.entrySet()) {
            String attr = entry.getKey();
            boolean higherBetter = HIGHER_IS_BETTER.contains(attr);
            boolean lowerBetter = LOWER_IS_BETTER.contains(attr);
            if (!higherBetter && !lowerBetter) continue;
            List<Object> values = entry.getValue();
            int bestIndex = -1;
            Comparable bestValue = null;
            for (int i = 0; i < values.size(); i++) {
                Object val = values.get(i);
                if (!(val instanceof Comparable)) continue;
                Comparable cval = (Comparable) val;
                if (bestValue == null
                        || (higherBetter && cval.compareTo(bestValue) > 0)
                        || (lowerBetter && cval.compareTo(bestValue) < 0)) {
                    bestValue = cval;
                    bestIndex = i;
                }
            }
            if (bestIndex >= 0) {
                highlights.put(attr, new ComparisonDetailResponse.HighlightInfo(
                        bestValue, versions.get(bestIndex).versionId()));
            }
        }
        return highlights;
    }
}
