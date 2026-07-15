package io.github.hevymcp.hevy.model;

import java.util.List;

public record RoutineFolderPage(
        Integer page,
        Integer pageCount,
        List<RoutineFolder> routineFolders) {
}
