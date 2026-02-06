package com.jauharxlr.springbase.admin.controller;

import com.jauharxlr.springbase.common.dto.ErrorResponse;
import com.jauharxlr.springbase.engine.service.DynamicDbService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/v1")
@RequiredArgsConstructor
@Tag(name = "Admin / AI-First Workflow", description = "Privileged endpoints for database management and AI integrations. Requires 'service_role' or administrative JWT.")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final DynamicDbService dynamicDbService;

    @Operation(
        summary = "SupaShell SQL Editor",
        description = "Executes raw SQL statements directly against the database. " +
                      "**Security Warning**: This endpoint bypasses all ownership security and project isolation. " +
                      "It is intended for administrative migrations, maintenance, or initial project setup. " +
                      "Supported statements: CREATE, ALTER, DROP, TRUNCATE, etc."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "SQL successfully executed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT required",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - 'service_role' required",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "SQL syntax error or database constraint violation",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/sql")
    public ResponseEntity<Object> executeSql(@RequestBody SqlRequest request) {
        dynamicDbService.executeRawSql(request.getSql());
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "JSON-to-DDL Schema Apply",
        description = "Accepts a high-level JSON representation of a database schema and applies it. " +
                      "This is specifically designed for AI coding assistants to generate backend schemas on the fly. " +
                      "The service will automatically create tables and columns as specified in the payload. " +
                      "If a table already exists, it will use 'CREATE TABLE IF NOT EXISTS' semantics."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Schema successfully applied"),
        @ApiResponse(responseCode = "400", description = "Invalid JSON schema definition",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - 'service_role' required",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Database error during execution",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/schema/apply")
    public ResponseEntity<Object> applySchema(@RequestBody SchemaDefinition schema) {
        // Simple JSON to DDL converter
        for (TableDefinition table : schema.getTables()) {
            StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
                    .append(table.getName())
                    .append(" (");
            
            List<String> colDefs = new java.util.ArrayList<>();
            for (ColumnDefinition col : table.getColumns()) {
                String def = col.getName() + " " + col.getType();
                if (col.isPrimaryKey()) def += " PRIMARY KEY";
                if (!col.isNullable()) def += " NOT NULL";
                if (col.getDefaultValue() != null) def += " DEFAULT " + col.getDefaultValue();
                colDefs.add(def);
            }
            sql.append(String.join(", ", colDefs)).append(")");
            dynamicDbService.executeRawSql(sql.toString());
        }
        return ResponseEntity.ok().build();
    }

    @Data
    @Schema(description = "Request object for SupaShell SQL execution")
    public static class SqlRequest {
        @Schema(description = "The raw SQL query to execute", example = "CREATE TABLE profile (id UUID PRIMARY KEY, bio TEXT, user_id UUID)")
        private String sql;
    }

    @Data
    @Schema(description = "JSON representation of a complete database schema")
    public static class SchemaDefinition {
        @Schema(description = "List of tables to be created")
        private List<TableDefinition> tables;
    }

    @Data
    @Schema(description = "Table structure definition")
    public static class TableDefinition {
        @Schema(description = "Name of the table", example = "tasks")
        private String name;
        @Schema(description = "List of columns for this table")
        private List<ColumnDefinition> columns;
    }

    @Data
    @Schema(description = "Column structure definition")
    public static class ColumnDefinition {
        @Schema(description = "Name of the column", example = "title")
        private String name;
        @Schema(description = "SQL data type of the column", example = "VARCHAR(255)")
        private String type;
        @Schema(description = "Whether the column is a primary key", example = "false")
        private boolean primaryKey;
        @Schema(description = "Whether the column can contain NULL values", example = "true")
        private boolean nullable = true;
        @Schema(description = "Default value for the column", example = "'Untitled'")
        private String defaultValue;
    }
}
