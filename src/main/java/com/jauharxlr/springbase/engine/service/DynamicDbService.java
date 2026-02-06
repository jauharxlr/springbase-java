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
            Integer count = jdbcTemplate.queryForObject(sql, Map.of("t", tableName.toUpperCase(), "c", columnName.toUpperCase()), Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
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
    
    public void executeRawSql(String sql) {
        jdbcTemplate.getJdbcTemplate().execute(sql);
    }
}
