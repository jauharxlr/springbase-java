package com.jauharxlr.springbase.actions.dto;

import lombok.Data;
import java.util.Map;

@Data
public class ActionOperation {
    private String table;
    private String op; // INSERT, UPDATE, DELETE, SELECT, PRE_CONDITION
    private Map<String, Object> data;
    private Map<String, String> filters;
    private String condition; // e.g., "balance >= 100"
}
