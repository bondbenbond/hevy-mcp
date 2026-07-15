package io.github.hevymcp.hevy.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.hevymcp.hevy.model.RoutineSet.RepRange;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RoutineUpdateRequest(RoutineUpdate routine) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RoutineUpdate(String title, @Nullable String notes, List<ExerciseUpdate> exercises) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ExerciseUpdate(
            String exerciseTemplateId,
            @Nullable Integer supersetId,
            @Nullable Integer restSeconds,
            @Nullable String notes,
            List<SetUpdate> sets) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SetUpdate(
            String type,
            @Nullable BigDecimal weightKg,
            @Nullable Integer reps,
            @Nullable Integer distanceMeters,
            @Nullable Integer durationSeconds,
            @Nullable BigDecimal customMetric,
            @Nullable RepRange repRange) {
    }
}
