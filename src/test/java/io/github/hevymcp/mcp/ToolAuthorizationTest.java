package io.github.hevymcp.mcp;

import io.github.hevymcp.hevy.HevyClient;
import io.github.hevymcp.hevy.model.ExerciseTemplate;
import io.github.hevymcp.hevy.model.ExerciseTemplatePage;
import io.github.hevymcp.hevy.model.ExerciseTemplateSearchResult;
import io.github.hevymcp.hevy.model.ExerciseHistory;
import io.github.hevymcp.hevy.model.RoutineFolder;
import io.github.hevymcp.hevy.model.RoutineFolderPage;
import io.github.hevymcp.hevy.model.Routine;
import io.github.hevymcp.hevy.model.RoutinePage;
import io.github.hevymcp.hevy.model.RoutineUpdateRequest;
import io.github.hevymcp.hevy.model.Workout;
import io.github.hevymcp.hevy.model.WorkoutPage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ToolAuthorizationTest {

    @Test
    void workoutToolsDelegateToClient() {
        HevyClient client = mock(HevyClient.class);
        WorkoutPage page = new WorkoutPage(1, 1, List.of());
        Workout workout = new Workout("w1", "Test", null, null, null, null, null, null, List.of());
        when(client.getWorkouts(1, 10)).thenReturn(page);
        when(client.getWorkout("w1")).thenReturn(workout);
        WorkoutTools tools = new WorkoutTools(client);

        tools.getWorkouts(null, null);
        tools.getWorkout("w1");

        verify(client).getWorkouts(1, 10);
        verify(client).getWorkout("w1");
    }

    @Test
    void routineToolsDelegateToClient() {
        HevyClient client = mock(HevyClient.class);
        RoutinePage page = new RoutinePage(1, 1, List.of());
        Routine routine = new Routine("r1", "Test", null, null, null, List.of());
        RoutineUpdateRequest update = new RoutineUpdateRequest(
                new RoutineUpdateRequest.RoutineUpdate("Test", null, List.of()));
        when(client.getRoutines(1, 10)).thenReturn(page);
        when(client.getRoutine("r1")).thenReturn(routine);
        when(client.updateRoutine("r1", update)).thenReturn(routine);
        RoutineTools tools = new RoutineTools(client);

        tools.getRoutines(null, null);
        tools.getRoutine("r1");
        tools.updateRoutine("r1", update);

        verify(client).getRoutines(1, 10);
        verify(client).getRoutine("r1");
        verify(client).updateRoutine("r1", update);
    }

    @Test
    void exerciseTemplateToolsDelegateToClient() {
        HevyClient client = mock(HevyClient.class);
        ExerciseTemplate template = new ExerciseTemplate(
                "template-1", "Incline Bench Press", "weight_reps", "chest",
                List.of("triceps"), "dumbbell", false);
        ExerciseTemplatePage page = new ExerciseTemplatePage(1, 1, List.of(template));
        ExerciseTemplateSearchResult search = new ExerciseTemplateSearchResult(
                "incline", 1, List.of(template));
        when(client.getExerciseTemplates(1, 100)).thenReturn(page);
        when(client.searchExerciseTemplates("incline", 25)).thenReturn(search);
        when(client.getExerciseTemplate("template-1")).thenReturn(template);
        ExerciseTemplateTools tools = new ExerciseTemplateTools(client);

        tools.getExerciseTemplates(null, null);
        tools.searchExerciseTemplates("incline", null);
        tools.getExerciseTemplate("template-1");

        verify(client).getExerciseTemplates(1, 100);
        verify(client).searchExerciseTemplates("incline", 25);
        verify(client).getExerciseTemplate("template-1");
    }

    @Test
    void exerciseHistoryToolsDelegateToClient() {
        HevyClient client = mock(HevyClient.class);
        when(client.getExerciseHistory("template-1", null, null))
                .thenReturn(new ExerciseHistory(List.of()));
        ExerciseHistoryTools tools = new ExerciseHistoryTools(client);

        tools.getExerciseHistory("template-1", null, null);

        verify(client).getExerciseHistory("template-1", null, null);
    }

    @Test
    void routineFolderToolsDelegateToClient() {
        HevyClient client = mock(HevyClient.class);
        RoutineFolder folder = new RoutineFolder(42L, 0, "Push Pull", "updated", "created");
        when(client.getRoutineFolders(1, 10))
                .thenReturn(new RoutineFolderPage(1, 1, List.of(folder)));
        when(client.getRoutineFolder("42")).thenReturn(folder);
        RoutineFolderTools tools = new RoutineFolderTools(client);

        tools.getRoutineFolders(null, null);
        tools.getRoutineFolder("42");

        verify(client).getRoutineFolders(1, 10);
        verify(client).getRoutineFolder("42");
    }
}
