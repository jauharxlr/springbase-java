package com.jauharxlr.springbase.automation.controller;

import com.jauharxlr.springbase.engine.service.DynamicDbService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/bridge")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "SupaSpring Bridge", description = "Endpoints for AiLytics service worker integration")
public class BridgeController {

    private final DynamicDbService dbService;

    @Operation(summary = "Callback for AiLytics completion")
    @PostMapping("/callback")
    public ResponseEntity<Map<String, String>> handleCallback(@RequestBody Map<String, Object> payload) {
        log.info("Received callback from AiLytics: {}", payload);

        String externalId = (String) payload.get("externalId");
        String status = (String) payload.get("status");
        String resultId = (String) payload.get("resultId");
        String error = (String) payload.get("errorMessage");

        if (externalId != null && externalId.contains(":")) {
            String[] parts = externalId.split(":");
            String tableName = parts[0];
            String recordId = parts[1];

            log.info("Updating SupaSpring record: {} in table: {} with result: {}", recordId, tableName, resultId);

            // Generic update: find a column named 'status' or 'ai_result'
            // For now, we update 'status' and 'ai_metadata' (if they exist)
            Map<String, Object> update = Map.of(
                "ai_status", status,
                "ai_result_id", resultId != null ? resultId : "",
                "ai_error", error != null ? error : ""
            );

            try {
                // Note: projectRef is usually part of the context, 
                // in a real bridge we would verify security, but for now we assume trusted internal bridge.
                // This is a placeholder for actual record update logic
                log.info("Record {} successfully processed by AI", recordId);
            } catch (Exception e) {
                log.error("Failed to update record: {}", e.getMessage());
            }
        }

        return ResponseEntity.ok(Map.of("message", "Callback processed"));
    }
}
