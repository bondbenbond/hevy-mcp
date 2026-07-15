package io.github.hevymcp.mcp;

import io.github.hevymcp.hevy.HevyClient;
import io.github.hevymcp.hevy.model.ExerciseTemplate;
import io.github.hevymcp.hevy.model.ExerciseTemplatePage;
import io.github.hevymcp.hevy.model.ExerciseTemplateSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class ExerciseTemplateTools {

    private static final Logger log = LoggerFactory.getLogger(ExerciseTemplateTools.class);
    private final HevyClient hevyClient;

    public ExerciseTemplateTools(HevyClient hevyClient) {
        this.hevyClient = hevyClient;
    }

    @McpTool(
            name = "get_exercise_templates",
            description = "List exercise templates available in Hevy. Exercise template IDs are required when adding or replacing exercises in routines.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = true))
    @PreAuthorize("hasAuthority('SCOPE_read:exercise_templates')")
    public ExerciseTemplatePage getExerciseTemplates(
            @McpToolParam(description = "Page number, starting at 1", required = false) Integer page,
            @McpToolParam(description = "Items per page, from 1 to 100", required = false) Integer pageSize) {
        log.info("MCP tool invoked tool=get_exercise_templates");
        return hevyClient.getExerciseTemplates(page == null ? 1 : page, pageSize == null ? 100 : pageSize);
    }

    @McpTool(
            name = "search_exercise_templates",
            description = "Search all Hevy exercise templates by name, ID, exercise type, muscle group, or equipment. Use this before changing routine exercises so valid template IDs are used.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = true))
    @PreAuthorize("hasAuthority('SCOPE_read:exercise_templates')")
    public ExerciseTemplateSearchResult searchExerciseTemplates(
            @McpToolParam(description = "Case-insensitive search text, such as incline bench, chest, or dumbbell", required = true)
            String query,
            @McpToolParam(description = "Maximum matches to return, from 1 to 100", required = false)
            Integer maxResults) {
        log.info("MCP tool invoked tool=search_exercise_templates");
        return hevyClient.searchExerciseTemplates(query, maxResults == null ? 25 : maxResults);
    }

    @McpTool(
            name = "get_exercise_template",
            description = "Get one Hevy exercise template by ID. Use this to verify a template before adding it to or replacing it in a routine.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = true))
    @PreAuthorize("hasAuthority('SCOPE_read:exercise_templates')")
    public ExerciseTemplate getExerciseTemplate(
            @McpToolParam(description = "The Hevy exercise template ID", required = true) String exerciseTemplateId) {
        log.info("MCP tool invoked tool=get_exercise_template exerciseTemplateId={}", exerciseTemplateId);
        return hevyClient.getExerciseTemplate(exerciseTemplateId);
    }
}
