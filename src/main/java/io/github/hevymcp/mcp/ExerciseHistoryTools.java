package io.github.hevymcp.mcp;

import io.github.hevymcp.hevy.HevyClient;
import io.github.hevymcp.hevy.model.ExerciseHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class ExerciseHistoryTools {

    private static final Logger log = LoggerFactory.getLogger(ExerciseHistoryTools.class);
    private final HevyClient hevyClient;

    public ExerciseHistoryTools(HevyClient hevyClient) {
        this.hevyClient = hevyClient;
    }

    @McpTool(
            name = "get_exercise_history",
            description = "Get completed set history for an exercise template, optionally within a date range. Use this to evaluate progression, recent performance, and appropriate routine changes.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = true))
    @PreAuthorize("hasAuthority('SCOPE_read:exercise_history')")
    public ExerciseHistory getExerciseHistory(
            @McpToolParam(description = "The Hevy exercise template ID", required = true)
            String exerciseTemplateId,
            @McpToolParam(description = "Optional inclusive start timestamp in ISO 8601 format", required = false)
            String startDate,
            @McpToolParam(description = "Optional inclusive end timestamp in ISO 8601 format", required = false)
            String endDate) {
        log.info("MCP tool invoked tool=get_exercise_history exerciseTemplateId={}", exerciseTemplateId);
        return hevyClient.getExerciseHistory(exerciseTemplateId, startDate, endDate);
    }
}
