package com.jauharxlr.springbase.engine.controller;

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

    @Operation(
        summary = "Query a table",
        description = "Fetches records from a user-defined table. " +
                      "**Ownership Security**: If the table contains a 'user_id' or 'owner_id' column, the query is automatically filtered to return only records owned by the authenticated user. " +
                      "**Filters**: Supports PostgREST-style filters via query parameters (e.g., `column=eq.value`, `age=gt.25`). " +
                      "Available operators: eq, gt, gte, lt, lte, neq, like, ilike.",
        parameters = {
            @Parameter(name = "table", description = "The name of the table to query", required = true, in = ParameterIn.PATH),
            @Parameter(name = "select", description = "Comma-separated list of columns to return (future support)", in = ParameterIn.QUERY),
            @Parameter(name = "order", description = "Order results (future support)", in = ParameterIn.QUERY),
            @Parameter(name = "limit", description = "Limit number of records (future support)", in = ParameterIn.QUERY)
        }
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successful query", 
                     content = @Content(array = @ArraySchema(schema = @Schema(implementation = Map.class)))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT required"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Access denied by security policies"),
        @ApiResponse(responseCode = "404", description = "Table not found"),
        @ApiResponse(responseCode = "500", description = "Database error or malformed filter")
    })
    @GetMapping("/{table}")
    public ResponseEntity<List<Map<String, Object>>> get(
            @PathVariable String table,
            HttpServletRequest request) {
        
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String projectRef = (String) request.getAttribute("project_ref");
        
        return ResponseEntity.ok(dynamicDbService.select(table, projectRef, userId, request.getParameterMap()));
    }

    @Operation(
        summary = "Insert into a table",
        description = "Inserts a new record into a user-defined table. " +
                      "**Ownership Injection**: If the table contains a 'user_id' or 'owner_id' column, it is automatically populated with the authenticated user's UUID from the JWT."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Record successfully created"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT required"),
        @ApiResponse(responseCode = "400", description = "Malformed JSON or data type mismatch"),
        @ApiResponse(responseCode = "500", description = "Database error")
    })
    @PostMapping("/{table}")
    public ResponseEntity<Void> post(
            @PathVariable String table,
            @RequestBody Map<String, Object> data) {
        
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        dynamicDbService.insert(table, userId, data);
        return ResponseEntity.status(201).build();
    }
}
