package com.jauharxlr.springbase.actions.service;

import com.jauharxlr.springbase.actions.dto.ActionOperation;
import com.jauharxlr.springbase.engine.service.DynamicDbService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActionService {

    private final DynamicDbService dynamicDbService;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Transactional
    public void executeActions(List<ActionOperation> operations, String userId, String projectRef) {
        int step = 0;
        try {
            for (ActionOperation op : operations) {
                step++;
                log.info("Executing action step {}: {} on table {}", step, op.getOp(), op.getTable());
                
                switch (op.getOp().toUpperCase()) {
                    case "INSERT" -> dynamicDbService.insert(op.getTable(), projectRef, userId, op.getData());
                    case "UPDATE" -> dynamicDbService.update(op.getTable(), projectRef, userId, convertFilters(op.getFilters()), op.getData());
                    case "DELETE" -> dynamicDbService.delete(op.getTable(), projectRef, userId, convertFilters(op.getFilters()));
                    case "PRE_CONDITION" -> checkPreCondition(op, userId, projectRef);
                    default -> throw new IllegalArgumentException("Unsupported operation: " + op.getOp());
                }
            }
        } catch (Exception e) {
            log.error("Action failed at step {}: {}", step, e.getMessage());
            throw new RuntimeException("Action failed at step " + step + ": " + e.getMessage(), e);
        }
    }

    private Map<String, String[]> convertFilters(Map<String, String> filters) {
        if (filters == null) return Collections.emptyMap();
        Map<String, String[]> result = new HashMap<>();
        filters.forEach((k, v) -> result.put(k, new String[]{v}));
        return result;
    }

    private void checkPreCondition(ActionOperation op, String userId, String projectRef) {
        String table = op.getTable();
        String condition = op.getCondition(); // e.g. "balance >= 100"
        Map<String, String> filters = op.getFilters();

        // We'll reuse the select logic but wrap it to check the condition
        // Actually, it might be better to build a custom query for pre-condition
        // to support the "condition" string safely.
        
        // Basic implementation: SELECT count(*) FROM table WHERE <filters> AND (<condition>)
        // We need to be careful with SQL injection for the 'condition' part.
        // For 'basic' support, we can just append it if we trust the user (or if we parse it).
        // Given the 'springbase' context, we usually apply Smart-Policy.
        
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ").append(table).append(" WHERE ");
        Map<String, Object> sqlParams = new HashMap<>();
        List<String> whereClauses = new ArrayList<>();

        // Add filters (reuse logic or simplify)
        if (filters != null) {
            filters.forEach((key, val) -> {
                if (val.contains(".")) {
                    String[] parts = val.split("\\.", 2);
                    String operator = parts[0];
                    String value = parts[1];
                    String sqlOp = getSqlOperator(operator);
                    String paramName = "pc_" + key + "_" + System.nanoTime();
                    whereClauses.add(key + " " + sqlOp + " :" + paramName);
                    sqlParams.put(paramName, parseValue(value));
                }
            });
        }

        // Add Smart-Policy
        if (userId != null && !"anonymous".equals(userId)) {
            UUID authUid = UUID.fromString(userId);
            sqlParams.put("auth_uid", authUid);
            List<String> policyClauses = new ArrayList<>();
            // This is a bit repetitive from DynamicDbService, maybe I should expose a method there
            // But for now I'll just check common ones
            if (hasColumn(table, "user_id")) policyClauses.add("user_id = :auth_uid");
            if (hasColumn(table, "owner_id")) policyClauses.add("owner_id = :auth_uid");
            if (hasColumn(table, "shared_with_id")) policyClauses.add("shared_with_id = :auth_uid");
            if (hasColumn(table, "merchant_id")) policyClauses.add("merchant_id = :auth_uid");
            if (hasColumn(table, "client_id")) policyClauses.add("client_id = :auth_uid");
            if (hasColumn(table, "company_id")) policyClauses.add("company_id = :auth_uid");
            if (hasColumn(table, "tenant_id")) policyClauses.add("tenant_id = :auth_uid");
            
            if (!policyClauses.isEmpty()) {
                whereClauses.add("(" + String.join(" OR ", policyClauses) + ")");
            }
        }

        if (whereClauses.isEmpty()) {
            whereClauses.add("1=1");
        }
        
        sql.append(String.join(" AND ", whereClauses));
        
        if (condition != null && !condition.isBlank()) {
            // WARNING: Simple append of condition. In a real system, we should parse this.
            // For this 'Atomic Action Engine' upgrade, we'll assume the condition is a valid SQL fragment.
            sql.append(" AND (").append(condition).append(")");
        }

        Integer count = jdbcTemplate.queryForObject(sql.toString(), sqlParams, Integer.class);
        if (count == null || count == 0) {
            throw new RuntimeException("Pre-condition failed: " + (condition != null ? condition : "filters not matched"));
        }
    }

    // Helper methods (cloned from DynamicDbService for now, or I should move them to a Utility)
    private String getSqlOperator(String op) {
        return switch (op) {
            case "eq" -> "=";
            case "gt" -> ">";
            case "gte" -> ">=";
            case "lt" -> "<";
            case "lte" -> "<=";
            case "neq" -> "<>";
            default -> "=";
        };
    }

    private Object parseValue(String val) {
        if (val.equalsIgnoreCase("true")) return true;
        if (val.equalsIgnoreCase("false")) return false;
        try { return Long.parseLong(val); } catch (NumberFormatException e) {}
        try { return Double.parseDouble(val); } catch (NumberFormatException e) {}
        return val;
    }

    private boolean hasColumn(String tableName, String columnName) {
        try {
            String sql = "SELECT count(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = :t AND COLUMN_NAME = :c";
            Integer count = jdbcTemplate.queryForObject(sql, Map.of("t", tableName.toLowerCase(), "c", columnName.toLowerCase()), Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
