package io.github.hevymcp.mcp;

import io.github.hevymcp.hevy.model.Routine;
import io.github.hevymcp.hevy.model.ExerciseHistoryEntry;
import io.github.hevymcp.hevy.model.RoutineExercise;
import io.github.hevymcp.hevy.model.RoutinePage;
import io.github.hevymcp.hevy.model.RoutineSet;
import io.github.hevymcp.hevy.model.RoutineUpdateRequest;
import io.github.hevymcp.hevy.model.WorkoutSet;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.annotation.method.tool.utils.McpJsonSchemaGenerator;
import org.springframework.ai.util.JsonHelper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("deprecation")
class McpOutputSchemaTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void nullableRoutineFieldsAreOptionalAndOmittedFromStructuredOutput() throws Exception {
        var set = new RoutineSet(0, "normal", null, null, null, null, null, null, null);
        var exercise = new RoutineExercise(
                0, "Running", null, null, "template-1", null, List.of(set));
        var page = new RoutinePage(1, 1, List.of(
                new Routine("routine-1", "Cardio", null, "updated", "created", List.of(exercise))));

        String structuredOutput = new JsonHelper().toJson(page);

        assertThat(structuredOutput)
                .doesNotContain("weightKg", "reps", "repRange", "distanceMeters", "durationSeconds",
                        "rpe", "customMetric", "restSeconds", "notes", "supersetsId", "folderId");
        assertThat(requiredProperties(RoutineSet.class)).containsExactlyInAnyOrder("index", "type");
        assertThat(requiredProperties(RoutineExercise.class))
                .containsExactlyInAnyOrder("index", "title", "exerciseTemplateId", "sets");
        assertThat(requiredProperties(Routine.class))
                .containsExactlyInAnyOrder("id", "title", "updatedAt", "createdAt", "exercises");
    }

    @Test
    void nullableWorkoutAndRoutineUpdateFieldsAreOptional() throws Exception {
        assertThat(requiredProperties(WorkoutSet.class)).containsExactlyInAnyOrder("index", "type");
        assertThat(requiredProperties(RoutineUpdateRequest.SetUpdate.class)).containsExactlyInAnyOrder("type");
        assertThat(requiredProperties(RoutineUpdateRequest.ExerciseUpdate.class))
                .containsExactlyInAnyOrder("exerciseTemplateId", "sets");
        assertThat(requiredProperties(RoutineUpdateRequest.RoutineUpdate.class))
                .containsExactlyInAnyOrder("title", "exercises");
        assertThat(requiredProperties(ExerciseHistoryEntry.class)).containsExactlyInAnyOrder(
                "workoutId", "workoutTitle", "workoutStartTime", "workoutEndTime",
                "exerciseTemplateId", "setType");
    }

    private List<String> requiredProperties(Class<?> type) throws Exception {
        var schema = mapper.readTree(McpJsonSchemaGenerator.generateFromType(type));
        return schema.path("required").valueStream().map(node -> node.asText()).toList();
    }
}
