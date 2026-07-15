package io.github.hevymcp.mcp;

import io.github.hevymcp.hevy.HevyClient;
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
}
