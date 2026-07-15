package io.github.hevymcp.hevy.model;

import java.util.List;

public record ExerciseTemplatePage(
        Integer page,
        Integer pageCount,
        List<ExerciseTemplate> exerciseTemplates) {
}
