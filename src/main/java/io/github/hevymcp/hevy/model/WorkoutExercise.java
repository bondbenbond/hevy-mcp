package io.github.hevymcp.hevy.model;

import java.util.List;

public record WorkoutExercise(
        Integer index,
        String title,
        String notes,
        String exerciseTemplateId,
        Integer supersetsId,
        List<WorkoutSet> sets) {
}
