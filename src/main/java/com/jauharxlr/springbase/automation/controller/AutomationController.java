package com.jauharxlr.springbase.automation.controller;

import com.jauharxlr.springbase.automation.entity.AutomationRule;
import com.jauharxlr.springbase.automation.repository.AutomationRuleRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rest/v1/automations")
@RequiredArgsConstructor
@Tag(name = "Automations", description = "Manage visual automations and webhooks")
@SecurityRequirement(name = "bearerAuth")
public class AutomationController {

    private final AutomationRuleRepository automationRuleRepository;

    @GetMapping
    public ResponseEntity<List<AutomationRule>> listAutomations(HttpServletRequest request) {
        String projectRef = (String) request.getAttribute("project_ref");
        return ResponseEntity.ok(automationRuleRepository.findByProjectRef(projectRef));
    }

    @PostMapping
    public ResponseEntity<AutomationRule> createAutomation(@RequestBody AutomationRule rule, HttpServletRequest request) {
        String projectRef = (String) request.getAttribute("project_ref");
        rule.setProjectRef(projectRef);
        return ResponseEntity.status(201).body(automationRuleRepository.save(rule));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAutomation(@PathVariable Long id, HttpServletRequest request) {
        String projectRef = (String) request.getAttribute("project_ref");
        automationRuleRepository.findById(id).ifPresent(rule -> {
            if (rule.getProjectRef().equals(projectRef)) {
                automationRuleRepository.delete(rule);
            }
        });
        return ResponseEntity.ok().build();
    }
}
