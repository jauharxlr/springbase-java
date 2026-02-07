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
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public List<Map<String, Object>> select(String tableName, String projectRef, String userId, String companyId, String tenantId, Map<String, String[]> params) {
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
                    
                    if (companyId != null && hasColumn(tableName, "company_id")) {
                        sqlParams.put("auth_company_id", UUID.fromString(companyId));
                        policyClauses.add("company_id = :auth_company_id");
                    }
                    if (tenantId != null && hasColumn(tableName, "tenant_id")) {
                        sqlParams.put("auth_tenant_id", UUID.fromString(tenantId));
                        policyClauses.add("tenant_id = :auth_tenant_id");
                    }
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid UUID for security claims: {}", userId);
                }
            }

            if (!policyClauses.isEmpty()) {
                conditions.add("(" + String.join(" OR ", policyClauses) + ")");
            } else if (isAnon && !isPublic) {
                conditions.add("1=0"); // Restricted access
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
               hasColumn(tableName, "client_id") ||
               hasColumn(tableName, "company_id") ||
               hasColumn(tableName, "tenant_id");
    }

    public Map<String, Object> insert(String tableName, String projectRef, String userId, String companyId, String tenantId, Map<String, Object> data) {
        // Ownership injection for insert
        if (userId != null && !"anonymous".equals(userId)) {
            try {
                UUID authUid = UUID.fromString(userId);
                if (hasColumn(tableName, "user_id")) {
                    data.put("user_id", authUid);
                } else if (hasColumn(tableName, "owner_id")) {
                    data.put("owner_id", authUid);
                }
                
                if (companyId != null && hasColumn(tableName, "company_id") && !data.containsKey("company_id")) {
                    data.put("company_id", UUID.fromString(companyId));
                }
                if (tenantId != null && hasColumn(tableName, "tenant_id") && !data.containsKey("tenant_id")) {
                    data.put("tenant_id", UUID.fromString(tenantId));
                }
            } catch (IllegalArgumentException e) {
                log.warn("Invalid UUID for security claims: {}", userId);
            }
        }

        String columns = String.join(", ", data.keySet());
        String placeholders = data.keySet().stream().map(k -> ":" + k).collect(Collectors.joining(", "));
        String sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ")";
        
        org.springframework.jdbc.support.KeyHolder keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbcTemplate.update(sql, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource(data), keyHolder);
        
        Map<String, Object> result = new HashMap<>(data);
        if (keyHolder.getKeys() != null) {
            result.putAll(keyHolder.getKeys());
        }

        eventPublisher.publishEvent(new com.jauharxlr.springbase.engine.event.CrudEvent(
                com.jauharxlr.springbase.engine.event.CrudEvent.EventType.ON_INSERT, tableName, projectRef, userId, companyId, tenantId, result));
        
        return result;
    }

    private boolean hasColumn(String tableName, String columnName) {
        try {
            String sql = "SELECT count(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = UPPER(:t) AND COLUMN_NAME = UPPER(:c)";
            Integer count = jdbcTemplate.queryForObject(sql, Map.of("t", tableName, "c", columnName), Integer.class);
            if (count != null && count > 0) return true;
            
            sql = "SELECT count(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = LOWER(:t) AND COLUMN_NAME = LOWER(:c)";
            count = jdbcTemplate.queryForObject(sql, Map.of("t", tableName, "c", columnName), Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public void update(String tableName, String projectRef, String userId, String companyId, String tenantId, Map<String, String[]> params, Map<String, Object> data) {
        StringBuilder sql = new StringBuilder("UPDATE ").append(tableName).append(" SET ");
        Map<String, Object> sqlParams = new HashMap<>();
        
        List<String> sets = new ArrayList<>();
        data.forEach((key, value) -> {
            sets.add(key + " = :v_" + key);
            sqlParams.put("v_" + key, value);
        });
        sql.append(String.join(", ", sets));

        List<String> conditions = new ArrayList<>();
        // Policy logic for update
        if (userId != null && !"anonymous".equals(userId)) {
            try {
                UUID authUid = UUID.fromString(userId);
                sqlParams.put("auth_uid", authUid);
                
                List<String> policyClauses = new ArrayList<>();
                if (hasColumn(tableName, "user_id")) policyClauses.add("user_id = :auth_uid");
                if (hasColumn(tableName, "owner_id")) policyClauses.add("owner_id = :auth_uid");
                if (hasColumn(tableName, "shared_with_id")) policyClauses.add("shared_with_id = :auth_uid");
                if (hasColumn(tableName, "merchant_id")) policyClauses.add("merchant_id = :auth_uid");
                if (hasColumn(tableName, "client_id")) policyClauses.add("client_id = :auth_uid");
                
                if (companyId != null && hasColumn(tableName, "company_id")) {
                    sqlParams.put("auth_company_id", UUID.fromString(companyId));
                    policyClauses.add("company_id = :auth_company_id");
                }
                if (tenantId != null && hasColumn(tableName, "tenant_id")) {
                    sqlParams.put("auth_tenant_id", UUID.fromString(tenantId));
                    policyClauses.add("tenant_id = :auth_tenant_id");
                }
                
                if (!policyClauses.isEmpty()) {
                    conditions.add("(" + String.join(" OR ", policyClauses) + ")");
                }
            } catch (IllegalArgumentException e) {
                log.warn("Invalid UUID for security claims: {}", userId);
                conditions.add("1=0");
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

        eventPublisher.publishEvent(new com.jauharxlr.springbase.engine.event.CrudEvent(
                com.jauharxlr.springbase.engine.event.CrudEvent.EventType.ON_UPDATE, tableName, projectRef, userId, companyId, tenantId, data));
    }

    public void delete(String tableName, String projectRef, String userId, String companyId, String tenantId, Map<String, String[]> params) {
        StringBuilder sql = new StringBuilder("DELETE FROM ").append(tableName);
        Map<String, Object> sqlParams = new HashMap<>();
        
        List<String> conditions = new ArrayList<>();
        // Policy logic for delete
        if (userId != null && !"anonymous".equals(userId)) {
            try {
                UUID authUid = UUID.fromString(userId);
                sqlParams.put("auth_uid", authUid);
                
                List<String> policyClauses = new ArrayList<>();
                if (hasColumn(tableName, "user_id")) policyClauses.add("user_id = :auth_uid");
                if (hasColumn(tableName, "owner_id")) policyClauses.add("owner_id = :auth_uid");
                if (hasColumn(tableName, "shared_with_id")) policyClauses.add("shared_with_id = :auth_uid");
                if (hasColumn(tableName, "merchant_id")) policyClauses.add("merchant_id = :auth_uid");
                if (hasColumn(tableName, "client_id")) policyClauses.add("client_id = :auth_uid");
                
                if (companyId != null && hasColumn(tableName, "company_id")) {
                    sqlParams.put("auth_company_id", UUID.fromString(companyId));
                    policyClauses.add("company_id = :auth_company_id");
                }
                if (tenantId != null && hasColumn(tableName, "tenant_id")) {
                    sqlParams.put("auth_tenant_id", UUID.fromString(tenantId));
                    policyClauses.add("tenant_id = :auth_tenant_id");
                }
                
                if (!policyClauses.isEmpty()) {
                    conditions.add("(" + String.join(" OR ", policyClauses) + ")");
                }
            } catch (IllegalArgumentException e) {
                log.warn("Invalid UUID for security claims: {}", userId);
                conditions.add("1=0");
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

        eventPublisher.publishEvent(new com.jauharxlr.springbase.engine.event.CrudEvent(
                com.jauharxlr.springbase.engine.event.CrudEvent.EventType.ON_DELETE, tableName, projectRef, userId, companyId, tenantId, Map.of()));
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
        String sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA IN ('PUBLIC', 'public') AND TABLE_TYPE IN ('TABLE', 'BASE TABLE', 'VIEW')";
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

    public List<Map<String, Object>> queryRawSql(String sql) {
        return jdbcTemplate.queryForList(sql, new HashMap<>());
    }
}
