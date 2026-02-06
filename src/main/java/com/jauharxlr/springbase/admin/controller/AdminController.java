package com.jauharxlr.springbase.admin.controller;

import com.jauharxlr.springbase.engine.service.DynamicDbService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/v1")
@RequiredArgsConstructor
public class AdminController {

    private final DynamicDbService dynamicDbService;

    @PostMapping("/sql")
    public ResponseEntity<Object> executeSql(@RequestBody SqlRequest request) {
        dynamicDbService.executeRawSql(request.getSql());
        return ResponseEntity.ok().build();
    }

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
                if (col.isNullable() == false) def += " NOT NULL";
                if (col.getDefaultValue() != null) def += " DEFAULT " + col.getDefaultValue();
                colDefs.add(def);
            }
            sql.append(String.join(", ", colDefs)).append(")");
            dynamicDbService.executeRawSql(sql.toString());
        }
        return ResponseEntity.ok().build();
    }

    @Data
    public static class SqlRequest {
        private String sql;
    }

    @Data
    public static class SchemaDefinition {
        private List<TableDefinition> tables;
    }

    @Data
    public static class TableDefinition {
        private String name;
        private List<ColumnDefinition> columns;
    }

    @Data
    public static class ColumnDefinition {
        private String name;
        private String type;
        private boolean primaryKey;
        private boolean nullable = true;
        private String defaultValue;
    }
}
