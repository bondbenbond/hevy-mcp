package io.github.hevymcp.hevy.model;

import java.math.BigDecimal;

public record WorkoutSet(
        Integer index,
        String type,
        BigDecimal weightKg,
        Integer reps,
        Integer distanceMeters,
        Integer durationSeconds,
        BigDecimal rpe,
        BigDecimal customMetric) {
}
