package io.github.hevymcp.hevy.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RoutineExercise(
        Integer index,
        String title,
        @Nullable String restSeconds,
        @Nullable String notes,
        String exerciseTemplateId,
        @Nullable Integer supersetsId,
        List<RoutineSet> sets) {
}
