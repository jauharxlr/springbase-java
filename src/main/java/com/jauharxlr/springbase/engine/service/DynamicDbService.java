package com.jauharxlr.springbase.engine.service;

import com.jauharxlr.springbase.engine.entity.TableMetadata;
import com.jauharxlr.springbase.engine.repository.TableMetadataRepository;
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
    private final TableMetadataRepository tableMetadataRepository;

    public List<Map<String, Object>> select(String tableName, String projectRef, String userId, Map<String, String[]> params) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tableName);
        Map<String, Object> sqlParams = new HashMap<>();
        
        List<String> conditions = new ArrayList<>();
        
        // Smart-Policy Logic
        boolean isPublic = isTablePublic(tableName, projectRef);
        boolean isAnon = "anonymous".equals(userId);

        if (!(isPublic && isAnon)) {
            List<String> policyClauses = new ArrayList<>();
            
            if (userId != null && !isAnon) {
                try {
                    UUID authUid = UUID.fromString(userId);
                    sqlParams.put("auth_uid", authUid);

                    if (hasColumn(tableName, "user_id")) policyClauses.add("user_id = :auth_uid");
                    if (hasColumn(tableName, "owner_id")) policyClauses.add("owner_id = :auth_uid");
                    if (hasColumn(tableName, "shared_with_id")) policyClauses.add("shared_with_id = :auth_uid");
                    if (hasColumn(tableName, "merchant_id")) policyClauses.add("merchant_id = :auth_uid");
                    if (hasColumn(tableName, "client_id")) policyClauses.add("client_id = :auth_uid");
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid UUID for userId: {}", userId);
                }
            }

            if (!policyClauses.isEmpty()) {
                conditions.add("(" + String.join(" OR ", policyClauses) + ")");
            } else if (isAnon && !isPublic) {
                conditions.add("1=0"); // Restricted access
            } else if (!isAnon && !hasAnySecurityColumn(tableName)) {
                // If no security columns exist, we allow access (or maybe we should restrict?)
                // Default behavior for tables without user_id was full access in previous version.
            } else if (!isAnon) {
                // User is logged in, security columns exist, but none matched? 
                // The OR logic above handles this if policyClauses is not empty.
                // If policyClauses IS empty but security columns exist, it means userId was null/invalid.
            }
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

    private boolean isTablePublic(String tableName, String projectRef) {
        return tableMetadataRepository.findByTableNameAndProjectRef(tableName.toLowerCase(), projectRef)
                .map(TableMetadata::isPublicRead)
                .orElse(false);
    }

    private boolean hasAnySecurityColumn(String tableName) {
        return hasColumn(tableName, "user_id") || 
               hasColumn(tableName, "owner_id") || 
               hasColumn(tableName, "shared_with_id") || 
               hasColumn(tableName, "merchant_id") ||
               hasColumn(tableName, "client_id");
    }

    public void insert(String tableName, String userId, Map<String, Object> data) {
        // Ownership injection for insert
        if (userId != null && !"anonymous".equals(userId)) {
            UUID authUid = UUID.fromString(userId);
            if (hasColumn(tableName, "user_id")) {
                data.put("user_id", authUid);
            } else if (hasColumn(tableName, "owner_id")) {
                data.put("owner_id", authUid);
            }
        }

        String columns = String.join(", ", data.keySet());
        String placeholders = data.keySet().stream().map(k -> ":" + k).collect(Collectors.joining(", "));
        String sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ")";
        
        jdbcTemplate.update(sql, data);
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
        // Policy logic for update (same as select but usually more restrictive, 
        // here we use the same Smart-Policy logic)
        if (userId != null && !"anonymous".equals(userId)) {
            UUID authUid = UUID.fromString(userId);
            sqlParams.put("auth_uid", authUid);
            
            List<String> policyClauses = new ArrayList<>();
            if (hasColumn(tableName, "user_id")) policyClauses.add("user_id = :auth_uid");
            if (hasColumn(tableName, "owner_id")) policyClauses.add("owner_id = :auth_uid");
            if (hasColumn(tableName, "shared_with_id")) policyClauses.add("shared_with_id = :auth_uid");
            if (hasColumn(tableName, "merchant_id")) policyClauses.add("merchant_id = :auth_uid");
            if (hasColumn(tableName, "client_id")) policyClauses.add("client_id = :auth_uid");
            
            if (!policyClauses.isEmpty()) {
                conditions.add("(" + String.join(" OR ", policyClauses) + ")");
            }
        } else {
            conditions.add("1=0"); // Anon cannot update
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
        // Policy logic for delete
        if (userId != null && !"anonymous".equals(userId)) {
            UUID authUid = UUID.fromString(userId);
            sqlParams.put("auth_uid", authUid);
            
            List<String> policyClauses = new ArrayList<>();
            if (hasColumn(tableName, "user_id")) policyClauses.add("user_id = :auth_uid");
            if (hasColumn(tableName, "owner_id")) policyClauses.add("owner_id = :auth_uid");
            if (hasColumn(tableName, "shared_with_id")) policyClauses.add("shared_with_id = :auth_uid");
            if (hasColumn(tableName, "merchant_id")) policyClauses.add("merchant_id = :auth_uid");
            if (hasColumn(tableName, "client_id")) policyClauses.add("client_id = :auth_uid");
            
            if (!policyClauses.isEmpty()) {
                conditions.add("(" + String.join(" OR ", policyClauses) + ")");
            }
        } else {
            conditions.add("1=0"); // Anon cannot delete
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

    public List<Map<String, Object>> getTablesMetadata(String projectRef) {
        List<String> tableNames = getTables();
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (String name : tableNames) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("name", name);
            meta.put("is_public", isTablePublic(name, projectRef));
            result.add(meta);
        }
        return result;
    }

    public void createTable(String tableName, String projectRef, List<Map<String, Object>> columns, boolean isPublic) {
        StringBuilder sql = new StringBuilder("CREATE TABLE ").append(tableName).append(" (");
        List<String> colDefs = new ArrayList<>();
        
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
        
        sql.append(String.join(", ", colDefs)).append(")");
        jdbcTemplate.getJdbcTemplate().execute(sql.toString());

        // Save metadata
        TableMetadata metadata = TableMetadata.builder()
                .tableName(tableName.toLowerCase())
                .projectRef(projectRef)
                .isPublicRead(isPublic)
                .build();
        tableMetadataRepository.save(metadata);
    }

    public void executeRawSql(String sql) {
        jdbcTemplate.getJdbcTemplate().execute(sql);
    }
}
