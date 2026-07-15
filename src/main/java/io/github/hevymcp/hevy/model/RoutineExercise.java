package io.github.hevymcp.hevy.model;

import java.util.List;

public record RoutineExercise(
        Integer index,
        String title,
        String restSeconds,
        String notes,
        String exerciseTemplateId,
        Integer supersetsId,
        List<RoutineSet> sets) {
}
