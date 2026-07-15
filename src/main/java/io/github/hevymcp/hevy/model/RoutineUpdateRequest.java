package io.github.hevymcp.hevy.model;

import java.math.BigDecimal;
import java.util.List;

public record RoutineUpdateRequest(RoutineUpdate routine) {

    public record RoutineUpdate(String title, String notes, List<ExerciseUpdate> exercises) {
    }

    public record ExerciseUpdate(
            String exerciseTemplateId,
            Integer supersetId,
            Integer restSeconds,
            String notes,
            List<SetUpdate> sets) {
    }

    public record SetUpdate(
            String type,
            BigDecimal weightKg,
            Integer reps,
            Integer distanceMeters,
            Integer durationSeconds,
            BigDecimal customMetric,
            RoutineSet.RepRange repRange) {
    }
}
