package io.github.hevymcp.hevy.model;

import java.math.BigDecimal;

public record RoutineSet(
        Integer index,
        String type,
        BigDecimal weightKg,
        Integer reps,
        RepRange repRange,
        Integer distanceMeters,
        Integer durationSeconds,
        BigDecimal rpe,
        BigDecimal customMetric) {

    public record RepRange(Integer start, Integer end) {
    }
}
