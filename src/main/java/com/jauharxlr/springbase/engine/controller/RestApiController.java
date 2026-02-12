package com.jauharxlr.springbase.engine.controller;

import com.jauharxlr.springbase.common.dto.ErrorResponse;
import com.jauharxlr.springbase.engine.service.DynamicDbService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rest/v1")
@RequiredArgsConstructor
@Tag(name = "Dynamic CRUD", description = "Automatic REST endpoints for user-defined tables with ownership-based security")
@SecurityRequirement(name = "bearerAuth")
public class RestApiController {

    private final DynamicDbService dynamicDbService;

    @GetMapping("/tables")
    public ResponseEntity<List<Map<String, Object>>> listTables(HttpServletRequest request) {
        String projectRef = (String) request.getAttribute("project_ref");
        return ResponseEntity.ok(dynamicDbService.getTablesMetadata(projectRef));
    }

    @PostMapping("/tables")
    public ResponseEntity<Void> createTable(@RequestBody TableRequest request, HttpServletRequest httpRequest) {
        String projectRef = (String) httpRequest.getAttribute("project_ref");
        dynamicDbService.createTable(request.getName(), projectRef, request.getColumns(), request.isPublic());
        return ResponseEntity.status(201).build();
    }

    @lombok.Data
    public static class TableRequest {
        private String name;
        private List<Map<String, Object>> columns;
        private boolean isPublic;
    }

    @Operation(
        summary = "Query a table",
        description = "Fetches records from a user-defined table using PostgREST-style syntax. " +
                      "**Ownership Security**: If the table contains a 'user_id', 'owner_id', 'company_id' or 'tenant_id' column, the query is automatically filtered to return only records matching the authenticated context. " +
                      "**Filters**: Supports filters via query parameters (e.g., `?age=gt.25&status=eq.active`). " +
                      "**Operators**: " +
                      "- `eq`: Equals " +
                      "- `neq`: Not equals " +
                      "- `gt`: Greater than " +
                      "- `gte`: Greater than or equal " +
                      "- `lt`: Less than " +
                      "- `lte`: Less than or equal " +
                      "- `like`: Pattern matching (case-sensitive) " +
                      "- `ilike`: Pattern matching (case-insensitive)",
        parameters = {
            @Parameter(name = "table", description = "The name of the database table to query", required = true, in = ParameterIn.PATH, example = "tasks"),
            @Parameter(name = "select", description = "Columns to include in response (comma-separated). *Currently returns all columns*.", in = ParameterIn.QUERY, example = "id,title,status"),
            @Parameter(name = "order", description = "Sort order (e.g. `id.asc`, `created_at.desc`). *Coming soon*.", in = ParameterIn.QUERY),
            @Parameter(name = "limit", description = "Maximum number of records to return. *Coming soon*.", in = ParameterIn.QUERY, example = "10")
        }
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successful query. Returns an array of objects matching the criteria.", 
                     content = @Content(array = @ArraySchema(schema = @Schema(implementation = Map.class, example = "{\"id\": 1, \"title\": \"Buy milk\", \"user_id\": \"...\"}")))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT required",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - Access denied by security policies",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Table not found in the project schema",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Database error or malformed filter expression",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{table}")
    public ResponseEntity<List<Map<String, Object>>> get(
            @PathVariable String table,
            HttpServletRequest request) {
        
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String projectRef = (String) request.getAttribute("project_ref");
        String companyId = (String) request.getAttribute("company_id");
        String tenantId = (String) request.getAttribute("tenant_id");
        
        return ResponseEntity.ok(dynamicDbService.select(table, projectRef, userId, companyId, tenantId, request.getParameterMap()));
    }

    @PostMapping("/{table}")
    public ResponseEntity<Void> post(
            @PathVariable String table,
            HttpServletRequest request,
            @RequestBody @Schema(example = "{\"title\": \"Finish homework\", \"completed\": false}") Map<String, Object> data) {
        
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String projectRef = (String) request.getAttribute("project_ref");
        String companyId = (String) request.getAttribute("company_id");
        String tenantId = (String) request.getAttribute("tenant_id");

        dynamicDbService.insert(table, projectRef, userId, companyId, tenantId, data);
        return ResponseEntity.status(201).build();
    }

    @PatchMapping("/{table}")
    public ResponseEntity<Void> patch(
            @PathVariable String table,
            HttpServletRequest request,
            @RequestBody Map<String, Object> data) {
        
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String projectRef = (String) request.getAttribute("project_ref");
        String companyId = (String) request.getAttribute("company_id");
        String tenantId = (String) request.getAttribute("tenant_id");

        dynamicDbService.update(table, projectRef, userId, companyId, tenantId, request.getParameterMap(), data);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{table}")
    public ResponseEntity<Void> delete(
            @PathVariable String table,
            HttpServletRequest request) {
        
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String projectRef = (String) request.getAttribute("project_ref");
        String companyId = (String) request.getAttribute("company_id");
        String tenantId = (String) request.getAttribute("tenant_id");

        dynamicDbService.delete(table, projectRef, userId, companyId, tenantId, request.getParameterMap());
        return ResponseEntity.ok().build();
    }
}
