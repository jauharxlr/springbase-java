package com.jauharxlr.springbase.engine.event;

import lombok.Getter;
import java.util.Map;

@Getter
public class CrudEvent {
    public enum EventType { ON_INSERT, ON_UPDATE, ON_DELETE }

    private final EventType type;
    private final String tableName;
    private final String projectRef;
    private final String userId;
    private final Map<String, Object> data;

    public CrudEvent(EventType type, String tableName, String projectRef, String userId, Map<String, Object> data) {
        this.type = type;
        this.tableName = tableName;
        this.projectRef = projectRef;
        this.userId = userId;
        this.data = data;
    }
}
