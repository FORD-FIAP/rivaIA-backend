package com.ford.riva.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RateLimitConfig {

    private final int generalLimit;
    private final int authLimit;

    public RateLimitConfig(
            @Value("${rate-limit.general.requests-per-minute:60}") int generalLimit,
            @Value("${rate-limit.auth.requests-per-minute:10}") int authLimit
    ) {
        this.generalLimit = generalLimit;
        this.authLimit = authLimit;
    }

    public int getGeneralLimit() {
        return generalLimit;
    }

    public int getAuthLimit() {
        return authLimit;
    }

    public Bucket newGeneralBucket() {
        return buildBucket(generalLimit);
    }

    public Bucket newAuthBucket() {
        return buildBucket(authLimit);
    }

    private Bucket buildBucket(int requestsPerMinute) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(requestsPerMinute)
                .refillGreedy(requestsPerMinute, Duration.ofMinutes(1))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
