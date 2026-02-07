package com.jauharxlr.springbase.automation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jauharxlr.springbase.actions.dto.ActionOperation;
import com.jauharxlr.springbase.actions.service.ActionService;
import com.jauharxlr.springbase.automation.entity.AutomationRule;
import com.jauharxlr.springbase.automation.repository.AutomationRuleRepository;
import com.jauharxlr.springbase.engine.event.CrudEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AutomationService {

    private final AutomationRuleRepository automationRuleRepository;
    private final ActionService actionService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public AutomationService(AutomationRuleRepository automationRuleRepository, ActionService actionService, ObjectMapper objectMapper) {
        this.automationRuleRepository = automationRuleRepository;
        this.actionService = actionService;
        this.objectMapper = objectMapper;
        
        // Configure RestTemplate with timeouts
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 5s
        factory.setReadTimeout(5000);    // 5s
        this.restTemplate = new RestTemplate(factory);
    }

    @Async
    @EventListener
    public void handleCrudEvent(CrudEvent event) {
        log.info("Handling CRUD event: {} on table {} for project {}", event.getType(), event.getTableName(), event.getProjectRef());

        List<AutomationRule> rules = automationRuleRepository.findByProjectRefAndTableNameAndEvent(
                event.getProjectRef(), event.getTableName(), event.getType().name());

        for (AutomationRule rule : rules) {
            try {
                executeRule(rule, event);
            } catch (Exception e) {
                log.error("Failed to execute automation rule {}: {}", rule.getId(), e.getMessage());
            }
        }
    }

    private void executeRule(AutomationRule rule, CrudEvent event) throws Exception {
        log.info("Executing rule {} (Type: {})", rule.getId(), rule.getActionType());

        if ("WEBHOOK".equalsIgnoreCase(rule.getActionType())) {
            executeWebhook(rule.getConfiguration(), event.getData());
        } else if ("DB_ACTION".equalsIgnoreCase(rule.getActionType())) {
            executeDbAction(rule.getConfiguration(), event.getUserId(), event.getProjectRef(), event.getCompanyId(), event.getTenantId());
        }
    }

    private void executeWebhook(String url, Map<String, Object> data) {
        try {
            log.info("Sending webhook to {}", url);
            restTemplate.postForEntity(url, data, String.class);
        } catch (Exception e) {
            log.error("Webhook failed for URL {}: {}", url, e.getMessage());
        }
    }

    private void executeDbAction(String configJson, String userId, String projectRef, String companyId, String tenantId) throws Exception {
        List<ActionOperation> operations = objectMapper.readValue(configJson, new TypeReference<List<ActionOperation>>() {});
        actionService.executeActions(operations, userId, projectRef, companyId, tenantId);
    }
}
