package io.github.hevymcp.mcp;

import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import io.github.hevymcp.hevy.model.Routine;
import io.github.hevymcp.hevy.model.ExerciseHistoryEntry;
import io.github.hevymcp.hevy.model.RoutineExercise;
import io.github.hevymcp.hevy.model.RoutinePage;
import io.github.hevymcp.hevy.model.RoutineSet;
import io.github.hevymcp.mcp.model.RoutineUpdateInput;
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
                new Routine("routine-1", "Cardio", null, null, "updated", "created", List.of(exercise))));

        String structuredOutput = new JsonHelper().toJson(page);

        assertThat(structuredOutput)
                .doesNotContain("weightKg", "reps", "repRange", "distanceMeters", "durationSeconds",
                        "rpe", "customMetric", "restSeconds", "notes", "supersetId", "folderId");
        assertThat(requiredProperties(RoutineSet.class)).containsExactlyInAnyOrder("index", "type");
        assertThat(requiredProperties(RoutineExercise.class))
                .containsExactlyInAnyOrder("index", "title", "exerciseTemplateId", "sets");
        assertThat(requiredProperties(Routine.class))
                .containsExactlyInAnyOrder("id", "title", "updatedAt", "createdAt", "exercises");
    }

    @Test
    void nullableWorkoutAndRoutineUpdateFieldsAreOptional() throws Exception {
        assertThat(requiredProperties(WorkoutSet.class)).containsExactlyInAnyOrder("index", "type");
        assertThat(requiredProperties(RoutineUpdateInput.SetUpdate.class)).isEmpty();
        assertThat(requiredProperties(RoutineUpdateInput.ExerciseUpdate.class)).isEmpty();
        assertThat(requiredProperties(RoutineUpdateInput.RoutineUpdate.class)).isEmpty();
        assertThat(requiredProperties(ExerciseHistoryEntry.class)).containsExactlyInAnyOrder(
                "workoutId", "workoutTitle", "workoutStartTime", "workoutEndTime",
                "exerciseTemplateId", "setType");
    }

    @Test
    void actualUpdateToolSchemasAcceptConditionalSetsAndCanonicalRoutineOutput() throws Exception {
        var method = RoutineTools.class.getMethod(
                "updateRoutine", String.class, RoutineUpdateInput.class);
        String inputSchemaJson = McpJsonSchemaGenerator.generateForMethodInput(method);
        var inputSchemaNode = mapper.readTree(inputSchemaJson);
        var requiredNames = inputSchemaNode.findValues("required").stream()
                .flatMap(node -> node.valueStream()).map(node -> node.asText()).toList();

        assertThat(requiredNames).doesNotContain(
                "supersetId", "weightKg", "reps", "distanceMeters", "durationSeconds",
                "customMetric", "repRange", "rpe", "notes");

        var registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);
        var inputSchema = registry.getSchema(inputSchemaNode);
        assertThat(inputSchema.validate(mapper.readTree("""
                {"routineId":"r1","update":{"routine":{"exercises":[
                  {"exerciseTemplateId":"plank","sets":[{"type":"normal","durationSeconds":45}]},
                  {"exerciseTemplateId":"carry","sets":[{"type":"normal","weightKg":45.35,
                    "distanceMeters":30}]}
                ]}}}
                """))).isEmpty();

        String outputSchemaJson = McpJsonSchemaGenerator.generateFromType(method.getGenericReturnType());
        var outputSchema = registry.getSchema(mapper.readTree(outputSchemaJson));
        assertThat(outputSchema.validate(mapper.readTree("""
                {"id":"r1","title":"Squat Week 1","createdAt":"created","updatedAt":"updated",
                 "exercises":[{"index":0,"title":"Plank","exerciseTemplateId":"plank",
                   "sets":[{"index":0,"type":"normal","durationSeconds":45}]}]}
                """))).isEmpty();
    }

    private List<String> requiredProperties(Class<?> type) throws Exception {
        var schema = mapper.readTree(McpJsonSchemaGenerator.generateFromType(type));
        return schema.path("required").valueStream().map(node -> node.asText()).toList();
    }
}
