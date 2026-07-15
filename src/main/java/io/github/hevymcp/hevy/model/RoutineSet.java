package io.github.hevymcp.hevy.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RoutineSet(
        Integer index,
        String type,
        @Nullable BigDecimal weightKg,
        @Nullable Integer reps,
        @Nullable RepRange repRange,
        @Nullable Integer distanceMeters,
        @Nullable Integer durationSeconds,
        @Nullable BigDecimal rpe,
        @Nullable BigDecimal customMetric) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RepRange(@Nullable Integer start, @Nullable Integer end) {
    }
}
