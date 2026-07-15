package io.github.hevymcp.mcp;

import io.github.hevymcp.hevy.HevyClient;
import io.github.hevymcp.hevy.model.RoutineUpdateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig(ToolMethodSecurityTest.Config.class)
class ToolMethodSecurityTest {

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class Config {
        @Bean WorkoutTools workoutTools(HevyClient client) { return new WorkoutTools(client); }
        @Bean RoutineTools routineTools(HevyClient client) { return new RoutineTools(client); }
    }

    @MockitoBean
    HevyClient client;

    @Autowired
    WorkoutTools workoutTools;

    @Autowired
    RoutineTools routineTools;

    @Test
    @WithMockUser(authorities = "SCOPE_read:workouts")
    void workoutReadScopeCanReadWorkoutsButNotRoutines() {
        workoutTools.getWorkouts(1, 10);
        workoutTools.getWorkout("w1");
        assertThatThrownBy(() -> routineTools.getRoutines(1, 10)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(authorities = "SCOPE_read:routines")
    void routineReadScopeCannotWrite() {
        routineTools.getRoutines(1, 10);
        routineTools.getRoutine("r1");
        var update = new RoutineUpdateRequest(new RoutineUpdateRequest.RoutineUpdate("Test", null, List.of()));
        assertThatThrownBy(() -> routineTools.updateRoutine("r1", update)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(authorities = "SCOPE_write:routines")
    void routineWriteScopeDoesNotGrantReads() {
        var update = new RoutineUpdateRequest(new RoutineUpdateRequest.RoutineUpdate("Test", null, List.of()));
        routineTools.updateRoutine("r1", update);
        assertThatThrownBy(() -> routineTools.getRoutine("r1")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> workoutTools.getWorkouts(1, 10)).isInstanceOf(AccessDeniedException.class);
    }
}
