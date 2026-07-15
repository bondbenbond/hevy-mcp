package io.github.hevymcp.hevy.model;

import java.util.List;

public record WorkoutPage(Integer page, Integer pageCount, List<Workout> workouts) {
}
