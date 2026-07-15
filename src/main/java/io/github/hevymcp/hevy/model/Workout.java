package io.github.hevymcp.hevy.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Workout(
        String id,
        String title,
        @Nullable String routineId,
        @Nullable String description,
        String startTime,
        String endTime,
        String updatedAt,
        String createdAt,
        List<WorkoutExercise> exercises) {
}
