package com.jauharxlr.springbase.automation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jauharxlr.springbase.actions.dto.ActionOperation;
import com.jauharxlr.springbase.actions.service.ActionService;
import com.jauharxlr.springbase.automation.entity.AutomationRule;
import com.jauharxlr.springbase.automation.repository.AutomationRuleRepository;
import com.jauharxlr.springbase.engine.event.CrudEvent;
import com.jauharxlr.springbase.engine.service.DynamicDbService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AutomationService {

    private final AutomationRuleRepository automationRuleRepository;
    private final ActionService actionService;
    private final DynamicDbService dbService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public AutomationService(AutomationRuleRepository automationRuleRepository, 
                             ActionService actionService, 
                             DynamicDbService dbService,
                             ObjectMapper objectMapper) {
        this.automationRuleRepository = automationRuleRepository;
        this.actionService = actionService;
        this.dbService = dbService;
        this.objectMapper = objectMapper;
        
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
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

        switch (rule.getActionType().toUpperCase()) {
            case "WEBHOOK" -> executeWebhook(rule.getConfiguration(), event.getData());
            case "DB_ACTION" -> executeDbAction(rule.getConfiguration(), event.getUserId(), event.getProjectRef(), event.getCompanyId(), event.getTenantId());
            case "PROCESS_DOCUMENT" -> executeProcessDocument(rule.getConfiguration(), event);
            case "AI_PROCESS" -> executeAiProcess(rule.getConfiguration(), event.getData());
        }
    }

    private void executeAiProcess(String configJson, Map<String, Object> data) {
        try {
            // Configuration: { "targetUrl": "...", "actionName": "...", "apiKey": "..." }
            Map<String, String> config = objectMapper.readValue(configJson, new TypeReference<Map<String, String>>() {});
            
            log.info("Triggering AI_PROCESS for action: {}", config.get("actionName"));

            Map<String, Object> payload = Map.of(
                "action", config.get("actionName"),
                "data", data,
                "timestamp", System.currentTimeMillis()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (config.get("apiKey") != null) {
                headers.set("Authorization", "Bearer " + config.get("apiKey"));
            }

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity(config.get("targetUrl"), requestEntity, String.class);

        } catch (Exception e) {
            log.error("AI_PROCESS failed: {}", e.getMessage());
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

    private void executeProcessDocument(String configJson, CrudEvent event) {
        try {
            // Configuration expected: { "ailyticsUrl": "...", "action": "...", "fileColumn": "...", "username": "...", "password": "..." }
            Map<String, String> config = objectMapper.readValue(configJson, new TypeReference<Map<String, String>>() {});
            
            String fileUrl = (String) event.getData().get(config.get("fileColumn"));
            if (fileUrl == null) {
                log.warn("Document process skipped: file column {} is empty", config.get("fileColumn"));
                return;
            }

            log.info("Triggering AiLytics Document Process for file: {}", fileUrl);

            // SupaSpring Bridge Logic: Trigger AiLytics with callback
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new UrlResource(fileUrl));
            body.add("action", config.get("action"));
            body.add("username", config.get("username"));
            body.add("password", config.get("password"));
            body.add("callbackUrl", "http://supaspring:1890/api/v1/bridge/callback"); // Internal callback
            body.add("externalId", event.getTableName() + ":" + event.getData().get("id"));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(config.get("ailyticsUrl") + "/api/v1/process", requestEntity, String.class);

        } catch (Exception e) {
            log.error("Failed to trigger document processing: {}", e.getMessage());
        }
    }
}
