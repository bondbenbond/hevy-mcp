package io.github.hevymcp.hevy.model;

import java.util.List;

public record Routine(
        String id,
        String title,
        Long folderId,
        String updatedAt,
        String createdAt,
        List<RoutineExercise> exercises) {
}
