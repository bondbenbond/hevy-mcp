package io.github.hevymcp.hevy.model;

import java.util.List;

public record RoutinePage(Integer page, Integer pageCount, List<Routine> routines) {
}
