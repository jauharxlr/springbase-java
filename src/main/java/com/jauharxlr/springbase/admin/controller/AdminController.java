package com.jauharxlr.springbase.admin.controller;

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
@Tag(name = "Admin / AI-First Workflow", description = "Privileged endpoints for database management and AI integrations. Requires 'service_role'.")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final DynamicDbService dynamicDbService;

    @Operation(
        summary = "SupaShell SQL Editor",
        description = "Executes raw SQL statements directly against the database. " +
                      "**Security Warning**: This endpoint bypasses all ownership security and project isolation. " +
                      "It should only be used for administrative migrations or maintenance."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "SQL successfully executed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT required"),
        @ApiResponse(responseCode = "403", description = "Forbidden - 'service_role' required"),
        @ApiResponse(responseCode = "500", description = "SQL syntax error or database constraint violation")
    })
    @PostMapping("/sql")
    public ResponseEntity<Object> executeSql(@RequestBody SqlRequest request) {
        dynamicDbService.executeRawSql(request.getSql());
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "JSON-to-DDL Schema Apply",
        description = "Accepts a high-level JSON representation of a database schema and applies it. " +
                      "This is designed for AI coding assistants to generate backend schemas on the fly. " +
                      "It will automatically create tables and columns as specified."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Schema successfully applied"),
        @ApiResponse(responseCode = "400", description = "Invalid JSON schema definition"),
        @ApiResponse(responseCode = "403", description = "Forbidden - 'service_role' required"),
        @ApiResponse(responseCode = "500", description = "Database error during execution")
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
        @Schema(example = "CREATE TABLE users_ext (id UUID PRIMARY KEY, bio TEXT, user_id UUID)")
        private String sql;
    }

    @Data
    @Schema(description = "JSON representation of a database schema")
    public static class SchemaDefinition {
        private List<TableDefinition> tables;
    }

    @Data
    @Schema(description = "Table structure definition")
    public static class TableDefinition {
        @Schema(example = "tasks")
        private String name;
        private List<ColumnDefinition> columns;
    }

    @Data
    @Schema(description = "Column structure definition")
    public static class ColumnDefinition {
        @Schema(example = "title")
        private String name;
        @Schema(example = "VARCHAR(255)")
        private String type;
        private boolean primaryKey;
        private boolean nullable = true;
        @Schema(example = "'Untitled'")
        private String defaultValue;
    }
}
