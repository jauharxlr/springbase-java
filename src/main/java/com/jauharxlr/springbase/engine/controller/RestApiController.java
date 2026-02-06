package com.jauharxlr.springbase.engine.controller;

import com.jauharxlr.springbase.engine.service.DynamicDbService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rest/v1")
@RequiredArgsConstructor
public class RestApiController {

    private final DynamicDbService dynamicDbService;

    @GetMapping("/{table}")
    public ResponseEntity<List<Map<String, Object>>> get(
            @PathVariable String table,
            HttpServletRequest request) {
        
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String projectRef = (String) request.getAttribute("project_ref");
        
        return ResponseEntity.ok(dynamicDbService.select(table, projectRef, userId, request.getParameterMap()));
    }

    @PostMapping("/{table}")
    public ResponseEntity<Void> post(
            @PathVariable String table,
            @RequestBody Map<String, Object> data) {
        
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        dynamicDbService.insert(table, userId, data);
        return ResponseEntity.status(201).build();
    }
}
