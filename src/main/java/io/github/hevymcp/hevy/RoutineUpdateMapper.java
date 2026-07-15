package io.github.hevymcp.hevy;

import io.github.hevymcp.hevy.model.HevyUpdateRoutineRequest;
import io.github.hevymcp.hevy.model.Routine;
import io.github.hevymcp.hevy.model.RoutineExercise;
import io.github.hevymcp.hevy.model.RoutineSet;
import io.github.hevymcp.mcp.model.RoutineUpdateInput;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public final class RoutineUpdateMapper {

    public HevyUpdateRoutineRequest merge(Routine existing, RoutineUpdateInput input) {
        if (input == null || input.routine() == null) {
            throw invalid("a routine update body is required");
        }

        var update = input.routine();
        String title = nonBlankOr(update.title(), existing.title());
        if (title == null || title.isBlank()) {
            throw invalid("the routine title is required");
        }
        String notes = nonBlankOr(update.notes(), existing.notes());
        if (notes == null) {
            notes = title;
        }

        List<HevyUpdateRoutineRequest.ExerciseUpdate> exercises = update.exercises() == null
                ? mapExistingExercises(existing.exercises())
                : mergeExercises(existing.exercises(), update.exercises());
        return new HevyUpdateRoutineRequest(
                new HevyUpdateRoutineRequest.RoutineUpdate(title, notes, exercises));
    }

    private List<HevyUpdateRoutineRequest.ExerciseUpdate> mergeExercises(
            List<RoutineExercise> existing,
            List<RoutineUpdateInput.ExerciseUpdate> updates) {
        var merged = new ArrayList<HevyUpdateRoutineRequest.ExerciseUpdate>(updates.size());
        for (int index = 0; index < updates.size(); index++) {
            RoutineExercise current = index < existing.size() ? existing.get(index) : null;
            var update = updates.get(index);
            String templateId = nonBlankOr(update.exerciseTemplateId(),
                    current == null ? null : current.exerciseTemplateId());
            if (templateId == null) {
                throw invalid("exercise " + index + " requires an exercise template ID");
            }
            if (current != null && update.exerciseTemplateId() != null
                    && !update.exerciseTemplateId().equals(current.exerciseTemplateId())) {
                current = findByTemplate(existing, update.exerciseTemplateId());
            }
            merged.add(mergeExercise(current, update, templateId));
        }
        return List.copyOf(merged);
    }

    private HevyUpdateRoutineRequest.ExerciseUpdate mergeExercise(
            @Nullable RoutineExercise existing,
            RoutineUpdateInput.ExerciseUpdate update,
            String templateId) {
        Integer supersetId = update.supersetId();
        if (supersetId == null || supersetId == 0) {
            supersetId = existing == null ? null : existing.supersetId();
        }
        List<HevyUpdateRoutineRequest.SetUpdate> sets = update.sets() == null
                ? mapExistingSets(existing == null ? List.of() : existing.sets())
                : mergeSets(existing == null ? List.of() : existing.sets(), update.sets());
        return new HevyUpdateRoutineRequest.ExerciseUpdate(
                templateId,
                supersetId,
                firstNonNull(update.restSeconds(), existing == null ? null : existing.restSeconds()),
                nonBlankOr(update.notes(), existing == null ? null : existing.notes()),
                sets);
    }

    private List<HevyUpdateRoutineRequest.SetUpdate> mergeSets(
            List<RoutineSet> existing,
            List<RoutineUpdateInput.SetUpdate> updates) {
        var merged = new ArrayList<HevyUpdateRoutineRequest.SetUpdate>(updates.size());
        for (int index = 0; index < updates.size(); index++) {
            RoutineSet current = index < existing.size() ? existing.get(index) : null;
            var update = updates.get(index);
            String type = nonBlankOr(update.type(), current == null ? null : current.type());
            if (type == null) {
                throw invalid("set " + index + " requires a set type");
            }
            merged.add(new HevyUpdateRoutineRequest.SetUpdate(
                    type,
                    firstNonNull(update.weightKg(), current == null ? null : current.weightKg()),
                    firstNonNull(update.reps(), current == null ? null : current.reps()),
                    firstNonNull(update.distanceMeters(), current == null ? null : current.distanceMeters()),
                    firstNonNull(update.durationSeconds(), current == null ? null : current.durationSeconds()),
                    firstNonNull(update.customMetric(), current == null ? null : current.customMetric()),
                    mergeRepRange(current == null ? null : current.repRange(), update.repRange())));
        }
        return List.copyOf(merged);
    }

    private List<HevyUpdateRoutineRequest.ExerciseUpdate> mapExistingExercises(List<RoutineExercise> exercises) {
        return exercises.stream().map(exercise -> new HevyUpdateRoutineRequest.ExerciseUpdate(
                exercise.exerciseTemplateId(), exercise.supersetId(), exercise.restSeconds(), exercise.notes(),
                mapExistingSets(exercise.sets()))).toList();
    }

    private List<HevyUpdateRoutineRequest.SetUpdate> mapExistingSets(List<RoutineSet> sets) {
        return sets.stream().map(set -> new HevyUpdateRoutineRequest.SetUpdate(
                set.type(), set.weightKg(), set.reps(), set.distanceMeters(), set.durationSeconds(),
                set.customMetric(), mapRepRange(set.repRange()))).toList();
    }

    private HevyUpdateRoutineRequest.RepRange mergeRepRange(
            RoutineSet.RepRange existing,
            RoutineUpdateInput.RepRangeInput update) {
        if (update == null) {
            return mapRepRange(existing);
        }
        return new HevyUpdateRoutineRequest.RepRange(
                firstNonNull(update.start(), existing == null ? null : existing.start()),
                firstNonNull(update.end(), existing == null ? null : existing.end()));
    }

    private HevyUpdateRoutineRequest.RepRange mapRepRange(RoutineSet.RepRange range) {
        return range == null ? null : new HevyUpdateRoutineRequest.RepRange(range.start(), range.end());
    }

    private RoutineExercise findByTemplate(List<RoutineExercise> exercises, String templateId) {
        return exercises.stream().filter(exercise -> templateId.equals(exercise.exerciseTemplateId()))
                .findFirst().orElse(null);
    }

    private static <T> T firstNonNull(@Nullable T preferred, @Nullable T fallback) {
        return preferred == null ? fallback : preferred;
    }

    private static String nonBlankOr(@Nullable String preferred, @Nullable String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }

    private static HevyApiException invalid(String detail) {
        return new HevyApiException(HevyErrorCode.BAD_REQUEST,
                "Invalid routine update input: " + detail + ".");
    }
}
