package io.github.hevymcp.hevy.model;

import java.util.List;

public record ExerciseTemplateSearchResult(
        String query,
        Integer matchCount,
        List<ExerciseTemplate> exerciseTemplates) {
}
