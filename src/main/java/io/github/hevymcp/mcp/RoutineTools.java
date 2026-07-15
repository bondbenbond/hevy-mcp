package io.github.hevymcp.mcp;

import io.github.hevymcp.hevy.HevyClient;
import io.github.hevymcp.hevy.RoutineUpdateService;
import io.github.hevymcp.hevy.model.Routine;
import io.github.hevymcp.hevy.model.RoutinePage;
import io.github.hevymcp.mcp.model.RoutineUpdateInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class RoutineTools {

    private static final Logger log = LoggerFactory.getLogger(RoutineTools.class);
    private final HevyClient hevyClient;
    private final RoutineUpdateService routineUpdateService;

    public RoutineTools(HevyClient hevyClient, RoutineUpdateService routineUpdateService) {
        this.hevyClient = hevyClient;
        this.routineUpdateService = routineUpdateService;
    }

    @McpTool(
            name = "get_routines",
            description = "Get routines configured in Hevy. Use this to inspect the user's current programmed workouts before recommending or making routine changes.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = true))
    @PreAuthorize("hasAuthority('SCOPE_read:routines')")
    public RoutinePage getRoutines(
            @McpToolParam(description = "Page number, starting at 1", required = false) Integer page,
            @McpToolParam(description = "Items per page, from 1 to 10", required = false) Integer pageSize) {
        log.info("MCP tool invoked tool=get_routines");
        return hevyClient.getRoutines(page == null ? 1 : page, pageSize == null ? 10 : pageSize);
    }

    @McpTool(
            name = "get_routine",
            description = "Get a specific Hevy routine by routine ID. Always use this before updating an existing routine so the current routine structure and exercises are preserved.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = true))
    @PreAuthorize("hasAuthority('SCOPE_read:routines')")
    public Routine getRoutine(
            @McpToolParam(description = "The Hevy routine ID", required = true) String routineId) {
        log.info("MCP tool invoked tool=get_routine routineId={}", routineId);
        return hevyClient.getRoutine(routineId);
    }

    @McpTool(
            name = "update_routine",
            description = "Update an existing Hevy routine. The server reads the current routine, preserves omitted or blank optional values, sends a complete normalized replacement to Hevy, then reads and returns the canonical updated routine.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true, idempotentHint = true, openWorldHint = true))
    @PreAuthorize("hasAuthority('SCOPE_write:routines')")
    public Routine updateRoutine(
            @McpToolParam(description = "The Hevy routine ID", required = true) String routineId,
            @McpToolParam(description = "Requested routine changes. Omit exercises or sets to preserve them; when supplied, their list order defines the replacement order.", required = true)
            RoutineUpdateInput update) {
        log.info("MCP tool invoked tool=update_routine routineId={}", routineId);
        return routineUpdateService.updateRoutine(routineId, update);
    }
}
