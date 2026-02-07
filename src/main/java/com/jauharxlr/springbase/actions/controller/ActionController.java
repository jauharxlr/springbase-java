package com.jauharxlr.springbase.actions.controller;

import com.jauharxlr.springbase.actions.dto.ActionRequest;
import com.jauharxlr.springbase.actions.service.ActionService;
import com.jauharxlr.springbase.common.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/v1/actions")
@RequiredArgsConstructor
@Tag(name = "Atomic Actions", description = "Multi-step database operations executed in a single transaction")
@SecurityRequirement(name = "bearerAuth")
public class ActionController {

    private final ActionService actionService;

    @Operation(
        summary = "Execute atomic actions",
        description = "Executes a sequence of database operations (INSERT, UPDATE, DELETE, PRE_CONDITION) in a single transaction. " +
                      "If any operation fails, the entire sequence is rolled back."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "All actions executed successfully"),
        @ApiResponse(responseCode = "400", description = "Malformed request or action logic error",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Transaction rolled back due to failure",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/execute")
    public ResponseEntity<Void> execute(@RequestBody ActionRequest request, HttpServletRequest httpRequest) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String projectRef = (String) httpRequest.getAttribute("project_ref");
        String companyId = (String) httpRequest.getAttribute("company_id");
        String tenantId = (String) httpRequest.getAttribute("tenant_id");
        
        actionService.executeActions(request.getOperations(), userId, projectRef, companyId, tenantId);
        return ResponseEntity.ok().build();
    }
}
