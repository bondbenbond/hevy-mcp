package io.github.hevymcp.mcp;

import io.github.hevymcp.hevy.HevyClient;
import io.github.hevymcp.hevy.model.RoutineFolder;
import io.github.hevymcp.hevy.model.RoutineFolderPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class RoutineFolderTools {

    private static final Logger log = LoggerFactory.getLogger(RoutineFolderTools.class);
    private final HevyClient hevyClient;

    public RoutineFolderTools(HevyClient hevyClient) {
        this.hevyClient = hevyClient;
    }

    @McpTool(
            name = "get_routine_folders",
            description = "List routine folders available in Hevy. Use this to understand how the user's routines are grouped and ordered.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = true))
    @PreAuthorize("hasAuthority('SCOPE_read:routine_folders')")
    public RoutineFolderPage getRoutineFolders(
            @McpToolParam(description = "Page number, starting at 1", required = false) Integer page,
            @McpToolParam(description = "Items per page, from 1 to 10", required = false) Integer pageSize) {
        log.info("MCP tool invoked tool=get_routine_folders");
        return hevyClient.getRoutineFolders(page == null ? 1 : page, pageSize == null ? 10 : pageSize);
    }

    @McpTool(
            name = "get_routine_folder",
            description = "Get one Hevy routine folder by ID, including its name and order.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = true))
    @PreAuthorize("hasAuthority('SCOPE_read:routine_folders')")
    public RoutineFolder getRoutineFolder(
            @McpToolParam(description = "The Hevy routine folder ID", required = true) String folderId) {
        log.info("MCP tool invoked tool=get_routine_folder folderId={}", folderId);
        return hevyClient.getRoutineFolder(folderId);
    }
}
