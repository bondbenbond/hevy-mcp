package io.github.hevymcp.hevy.model;

import java.util.List;

public record Workout(
        String id,
        String title,
        String routineId,
        String description,
        String startTime,
        String endTime,
        String updatedAt,
        String createdAt,
        List<WorkoutExercise> exercises) {
}
