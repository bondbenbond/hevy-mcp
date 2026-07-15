package io.github.hevymcp.mcp;

import io.github.hevymcp.hevy.HevyClient;
import io.github.hevymcp.hevy.model.Workout;
import io.github.hevymcp.hevy.model.WorkoutPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class WorkoutTools {

    private static final Logger log = LoggerFactory.getLogger(WorkoutTools.class);
    private final HevyClient hevyClient;

    public WorkoutTools(HevyClient hevyClient) {
        this.hevyClient = hevyClient;
    }

    @McpTool(
            name = "get_workouts",
            description = "Get completed workouts from Hevy. Use this when analyzing workout history, training volume, exercise performance, or strength progression.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = true))
    @PreAuthorize("hasAuthority('SCOPE_read:workouts')")
    public WorkoutPage getWorkouts(
            @McpToolParam(description = "Page number, starting at 1", required = false) Integer page,
            @McpToolParam(description = "Items per page, from 1 to 10", required = false) Integer pageSize) {
        log.info("MCP tool invoked tool=get_workouts");
        return hevyClient.getWorkouts(page == null ? 1 : page, pageSize == null ? 10 : pageSize);
    }

    @McpTool(
            name = "get_workout",
            description = "Get a specific Hevy workout by workout ID. Use this when detailed exercise, set, rep, and weight information is needed for one workout.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = true))
    @PreAuthorize("hasAuthority('SCOPE_read:workouts')")
    public Workout getWorkout(
            @McpToolParam(description = "The Hevy workout ID", required = true) String workoutId) {
        log.info("MCP tool invoked tool=get_workout workoutId={}", workoutId);
        return hevyClient.getWorkout(workoutId);
    }
}
