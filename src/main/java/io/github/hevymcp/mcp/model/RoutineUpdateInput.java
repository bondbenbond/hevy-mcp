package io.github.hevymcp.mcp.model;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;

public record RoutineUpdateInput(RoutineUpdate routine) {

    public record RoutineUpdate(
            @Nullable String title,
            @Nullable String notes,
            @Nullable List<ExerciseUpdate> exercises) {
    }

    public record ExerciseUpdate(
            @Nullable String exerciseTemplateId,
            @Nullable Integer supersetId,
            @Nullable Integer restSeconds,
            @Nullable String notes,
            @Nullable List<SetUpdate> sets) {
    }

    public record SetUpdate(
            @Nullable String type,
            @Nullable BigDecimal weightKg,
            @Nullable Integer reps,
            @Nullable Integer distanceMeters,
            @Nullable Integer durationSeconds,
            @Nullable BigDecimal customMetric,
            @Nullable RepRangeInput repRange) {
    }

    public record RepRangeInput(@Nullable Integer start, @Nullable Integer end) {
    }
}
