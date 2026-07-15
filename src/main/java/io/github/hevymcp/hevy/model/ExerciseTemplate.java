package io.github.hevymcp.hevy.model;

import java.util.List;

public record ExerciseTemplate(
        String id,
        String title,
        String type,
        String primaryMuscleGroup,
        List<String> secondaryMuscleGroups,
        String equipmentCategory,
        Boolean isCustom) {
}
