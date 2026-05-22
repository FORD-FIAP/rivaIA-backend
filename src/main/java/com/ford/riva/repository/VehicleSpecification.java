package com.ford.riva.repository;

import com.ford.riva.dto.request.VehicleSearchRequest;
import com.ford.riva.model.Vehicle;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public class VehicleSpecification {

    private VehicleSpecification() {}

    public static Specification<Vehicle> fromSearchRequest(VehicleSearchRequest request) {
        Specification<Vehicle> spec = Specification.where(null);

        if (request.brandId() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.join("brand", JoinType.INNER).get("brandId"), request.brandId()));
        }
        if (request.categoryId() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.join("category", JoinType.INNER).get("categoryId"), request.categoryId()));
        }
        if (request.model() != null && !request.model().isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("model")), "%" + request.model().toLowerCase() + "%"));
        }
        if (request.modelYearMin() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("modelYear"), request.modelYearMin()));
        }
        if (request.modelYearMax() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("modelYear"), request.modelYearMax()));
        }
        if (request.priceMin() != null || request.priceMax() != null
                || request.fuelType() != null || request.drivetrain() != null) {
            spec = spec.and((root, query, cb) -> {
                var versionJoin = root.join("versions", JoinType.INNER);
                var predicates = cb.conjunction();

                if (request.priceMin() != null) {
                    predicates = cb.and(predicates,
                            cb.greaterThanOrEqualTo(versionJoin.get("price"), request.priceMin()));
                }
                if (request.priceMax() != null) {
                    predicates = cb.and(predicates,
                            cb.lessThanOrEqualTo(versionJoin.get("price"), request.priceMax()));
                }
                if (request.fuelType() != null || request.drivetrain() != null) {
                    var powertrainJoin = versionJoin.join("powertrain", JoinType.INNER);
                    if (request.fuelType() != null && !request.fuelType().isBlank()) {
                        predicates = cb.and(predicates,
                                cb.equal(cb.lower(powertrainJoin.get("fuelType")),
                                        request.fuelType().toLowerCase()));
                    }
                    if (request.drivetrain() != null && !request.drivetrain().isBlank()) {
                        predicates = cb.and(predicates,
                                cb.equal(cb.lower(powertrainJoin.get("drivetrain")),
                                        request.drivetrain().toLowerCase()));
                    }
                }
                query.distinct(true);
                return predicates;
            });
        }

        return spec;
    }
}
