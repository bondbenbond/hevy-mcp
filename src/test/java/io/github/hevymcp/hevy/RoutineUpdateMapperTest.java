package io.github.hevymcp.hevy;

import io.github.hevymcp.hevy.model.HevyUpdateRoutineRequest;
import io.github.hevymcp.hevy.model.Routine;
import io.github.hevymcp.hevy.model.RoutineExercise;
import io.github.hevymcp.hevy.model.RoutineSet;
import io.github.hevymcp.mcp.model.RoutineUpdateInput;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoutineUpdateMapperTest {

    private final RoutineUpdateMapper mapper = new RoutineUpdateMapper();
    private final JsonMapper json = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build();

    @Test
    void mapsConditionalMetricsAndOmitsUnrelatedNullFields() throws Exception {
        var input = input("Updated notes", List.of(
                exercise("squat", null, set("normal", "70.3069009625", 5, null, null)),
                exercise("plank", null, set("normal", null, null, null, 45)),
                exercise("carry", 0, set("normal", "45.3592909436", null, 30, null))));

        HevyUpdateRoutineRequest result = mapper.merge(existingRoutine(), input);
        String serialized = json.writeValueAsString(result);

        assertThat(serialized)
                .contains("\"weight_kg\":70.3069009625", "\"reps\":5")
                .contains("\"duration_seconds\":45")
                .contains("\"weight_kg\":45.3592909436", "\"distance_meters\":30")
                .doesNotContain("\"superset_id\":0", "duration_seconds\":null",
                        "distance_meters\":null", "custom_metric\":null", "rep_range\":null");
        assertThat(result.routine().exercises().get(0).sets().getFirst().weightKg().doubleValue())
                .isCloseTo(70.3069009625, org.assertj.core.data.Offset.offset(0.0000000001));
    }

    @Test
    void preservesRealSupersetNotesUnchangedSetsAndOrder() {
        var existing = existingRoutine();
        var input = new RoutineUpdateInput(new RoutineUpdateInput.RoutineUpdate(
                null, "  ", List.of(
                new RoutineUpdateInput.ExerciseUpdate("squat", null, null, null, null),
                new RoutineUpdateInput.ExerciseUpdate("plank", null, 120, null, null),
                new RoutineUpdateInput.ExerciseUpdate("carry", null, null, null, null))));

        var result = mapper.merge(existing, input).routine();

        assertThat(result.title()).isEqualTo(existing.title());
        assertThat(result.notes()).isEqualTo(existing.notes());
        assertThat(result.exercises()).extracting(HevyUpdateRoutineRequest.ExerciseUpdate::exerciseTemplateId)
                .containsExactly("squat", "plank", "carry");
        assertThat(result.exercises().getFirst().supersetId()).isEqualTo(12);
        assertThat(result.exercises().getFirst().sets()).hasSize(1);
        assertThat(result.exercises().get(1).restSeconds()).isEqualTo(120);
    }

    @Test
    void blankNotesFallBackToTitleWhenNoExistingNotesExist() {
        Routine existing = new Routine("r1", "Fallback title", null, null,
                "updated", "created", List.of());
        var input = new RoutineUpdateInput(new RoutineUpdateInput.RoutineUpdate(null, "", null));

        assertThat(mapper.merge(existing, input).routine().notes()).isEqualTo("Fallback title");
    }

    private Routine existingRoutine() {
        return new Routine("r1", "Squat Week 1", "Existing notes", null, "updated", "created", List.of(
                new RoutineExercise(0, "Squat", 180, "Squat notes", "squat", 12,
                        List.of(new RoutineSet(0, "normal", new BigDecimal("60"), 5,
                                null, null, null, null, null))),
                new RoutineExercise(1, "Plank", 60, null, "plank", null,
                        List.of(new RoutineSet(0, "normal", null, null,
                                null, null, 30, null, null))),
                new RoutineExercise(2, "Carry", 90, null, "carry", null,
                        List.of(new RoutineSet(0, "normal", new BigDecimal("40"), null,
                                null, 20, null, null, null)))));
    }

    private RoutineUpdateInput input(String notes, List<RoutineUpdateInput.ExerciseUpdate> exercises) {
        return new RoutineUpdateInput(new RoutineUpdateInput.RoutineUpdate("Squat Week 1", notes, exercises));
    }

    private RoutineUpdateInput.ExerciseUpdate exercise(
            String templateId, Integer supersetId, RoutineUpdateInput.SetUpdate set) {
        return new RoutineUpdateInput.ExerciseUpdate(templateId, supersetId, null, null, List.of(set));
    }

    private RoutineUpdateInput.SetUpdate set(
            String type, String weight, Integer reps, Integer distance, Integer duration) {
        return new RoutineUpdateInput.SetUpdate(type, weight == null ? null : new BigDecimal(weight),
                reps, distance, duration, null, null);
    }
}
