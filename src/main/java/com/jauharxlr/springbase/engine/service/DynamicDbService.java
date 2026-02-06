package com.jauharxlr.springbase.engine.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DynamicDbService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> select(String tableName, String projectRef, String userId, Map<String, String[]> params) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tableName);
        Map<String, Object> sqlParams = new HashMap<>();
        
        List<String> conditions = new ArrayList<>();
        
        // Ownership injection
        if (hasColumn(tableName, "user_id")) {
            conditions.add("user_id = :ownerId");
            sqlParams.put("ownerId", UUID.fromString(userId));
        } else if (hasColumn(tableName, "owner_id")) {
            conditions.add("owner_id = :ownerId");
            sqlParams.put("ownerId", UUID.fromString(userId));
        }

        // PostgREST filters
        params.forEach((key, values) -> {
            if (!key.equals("select") && !key.equals("order") && !key.equals("limit")) {
                for (String val : values) {
                    if (val.contains(".")) {
                        String[] parts = val.split("\\.", 2);
                        String op = parts[0];
                        String actualVal = parts[1];
                        String sqlOp = getSqlOperator(op);
                        String paramName = "p_" + key + "_" + System.nanoTime();
                        conditions.add(key + " " + sqlOp + " :" + paramName);
                        sqlParams.put(paramName, parseValue(actualVal));
                    }
                }
            }
        });

        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }

        log.debug("Executing SQL: {} with params: {}", sql, sqlParams);
        return jdbcTemplate.queryForList(sql.toString(), sqlParams);
    }

    public void insert(String tableName, String userId, Map<String, Object> data) {
        // Ownership injection for insert
        if (hasColumn(tableName, "user_id")) {
            data.put("user_id", UUID.fromString(userId));
        } else if (hasColumn(tableName, "owner_id")) {
            data.put("owner_id", UUID.fromString(userId));
        }

        String columns = String.join(", ", data.keySet());
        String placeholders = data.keySet().stream().map(k -> ":" + k).collect(Collectors.joining(", "));
        String sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ")";
        
        jdbcTemplate.update(sql, data);
    }

    private boolean hasColumn(String tableName, String columnName) {
        try {
            // In a real app, we would cache this from metadata catalog
            String sql = "SELECT count(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = :t AND COLUMN_NAME = :c";
            Integer count = jdbcTemplate.queryForObject(sql, Map.of("t", tableName.toLowerCase(), "c", columnName.toLowerCase()), Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public void update(String tableName, String userId, Map<String, String[]> params, Map<String, Object> data) {
        StringBuilder sql = new StringBuilder("UPDATE ").append(tableName).append(" SET ");
        Map<String, Object> sqlParams = new HashMap<>();
        
        List<String> sets = new ArrayList<>();
        data.forEach((key, value) -> {
            sets.add(key + " = :v_" + key);
            sqlParams.put("v_" + key, value);
        });
        sql.append(String.join(", ", sets));

        List<String> conditions = new ArrayList<>();
        // Ownership injection
        if (hasColumn(tableName, "user_id")) {
            conditions.add("user_id = :ownerId");
            sqlParams.put("ownerId", UUID.fromString(userId));
        } else if (hasColumn(tableName, "owner_id")) {
            conditions.add("owner_id = :ownerId");
            sqlParams.put("ownerId", UUID.fromString(userId));
        }

        // PostgREST filters
        params.forEach((key, values) -> {
            if (!key.equals("select") && !key.equals("order") && !key.equals("limit")) {
                for (String val : values) {
                    if (val.contains(".")) {
                        String[] parts = val.split("\\.", 2);
                        String op = parts[0];
                        String actualVal = parts[1];
                        String sqlOp = getSqlOperator(op);
                        String paramName = "p_" + key + "_" + System.nanoTime();
                        conditions.add(key + " " + sqlOp + " :" + paramName);
                        sqlParams.put(paramName, parseValue(actualVal));
                    }
                }
            }
        });

        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }

        jdbcTemplate.update(sql.toString(), sqlParams);
    }

    public void delete(String tableName, String userId, Map<String, String[]> params) {
        StringBuilder sql = new StringBuilder("DELETE FROM ").append(tableName);
        Map<String, Object> sqlParams = new HashMap<>();
        
        List<String> conditions = new ArrayList<>();
        // Ownership injection
        if (hasColumn(tableName, "user_id")) {
            conditions.add("user_id = :ownerId");
            sqlParams.put("ownerId", UUID.fromString(userId));
        } else if (hasColumn(tableName, "owner_id")) {
            conditions.add("owner_id = :ownerId");
            sqlParams.put("ownerId", UUID.fromString(userId));
        }

        // PostgREST filters
        params.forEach((key, values) -> {
            for (String val : values) {
                if (val.contains(".")) {
                    String[] parts = val.split("\\.", 2);
                    String op = parts[0];
                    String actualVal = parts[1];
                    String sqlOp = getSqlOperator(op);
                    String paramName = "p_" + key + "_" + System.nanoTime();
                    conditions.add(key + " " + sqlOp + " :" + paramName);
                    sqlParams.put(paramName, parseValue(actualVal));
                }
            }
        });

        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }

        jdbcTemplate.update(sql.toString(), sqlParams);
    }

    private String getSqlOperator(String op) {
        return switch (op) {
            case "eq" -> "=";
            case "gt" -> ">";
            case "gte" -> ">=";
            case "lt" -> "<";
            case "lte" -> "<=";
            case "neq" -> "<>";
            case "like" -> "LIKE";
            case "ilike" -> "ILIKE";
            default -> "=";
        };
    }

    private Object parseValue(String val) {
        // Simple parser for numbers, booleans, or UUIDs
        if (val.equalsIgnoreCase("true")) return true;
        if (val.equalsIgnoreCase("false")) return false;
        try { return Long.parseLong(val); } catch (NumberFormatException e) {}
        try { return Double.parseDouble(val); } catch (NumberFormatException e) {}
        try { return UUID.fromString(val); } catch (IllegalArgumentException e) {}
        return val;
    }
    
    public List<String> getTables() {
        String sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_TYPE = 'TABLE'";
        return jdbcTemplate.queryForList(sql, Map.of(), String.class);
    }

    public void createTable(String tableName, List<Map<String, Object>> columns) {
        StringBuilder sql = new StringBuilder("CREATE TABLE ").append(tableName).append(" (");
        List<String> colDefs = new ArrayList<>();
        
        // Always add id if not present? Or let user define?
        // Let's assume user defines columns.
        for (Map<String, Object> col : columns) {
            String name = (String) col.get("name");
            String type = (String) col.get("type");
            Boolean isPrimaryKey = (Boolean) col.get("primaryKey");
            Boolean isNullable = (Boolean) col.get("nullable");
            
            String def = name + " " + type;
            if (isPrimaryKey != null && isPrimaryKey) def += " PRIMARY KEY";
            if (isNullable != null && !isNullable) def += " NOT NULL";
            colDefs.add(def);
        }
        
        // If no user_id or owner_id, maybe we should add one for ownership?
        // The prompt says "If the table contains a 'user_id' or 'owner_id' column...".
        // So we should probably let the user decide.
        
        sql.append(String.join(", ", colDefs)).append(")");
        jdbcTemplate.getJdbcTemplate().execute(sql.toString());
    }

    public void executeRawSql(String sql) {
        jdbcTemplate.getJdbcTemplate().execute(sql);
    }
}
