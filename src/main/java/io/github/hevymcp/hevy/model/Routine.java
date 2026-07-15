package io.github.hevymcp.hevy.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Routine(
        String id,
        String title,
        @Nullable String notes,
        @Nullable Long folderId,
        String updatedAt,
        String createdAt,
        List<RoutineExercise> exercises) {
}
