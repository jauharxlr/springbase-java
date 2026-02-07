package com.jauharxlr.springbase.automation.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sb_automation_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutomationRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String projectRef;
    private String tableName;
    private String event; // ON_INSERT, ON_UPDATE, ON_DELETE
    private String actionType; // WEBHOOK, DB_ACTION
    
    @Column(columnDefinition = "TEXT")
    private String configuration; // Webhook URL or Action JSON
}
