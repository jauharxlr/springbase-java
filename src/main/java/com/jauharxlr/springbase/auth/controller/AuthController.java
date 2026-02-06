package com.jauharxlr.springbase.auth.controller;

import com.jauharxlr.springbase.auth.dto.AuthResponse;
import com.jauharxlr.springbase.auth.dto.LoginRequest;
import com.jauharxlr.springbase.auth.dto.SignupRequest;
import com.jauharxlr.springbase.auth.service.AuthService;
import com.jauharxlr.springbase.common.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/v1")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration and authentication with multi-tenancy support")
public class AuthController {

    private final AuthService authService;

    @Operation(
        summary = "Register a new user",
        description = "Creates a new user record in the system. Users are logically isolated by 'projectRef'. " +
                      "By default, new users are assigned the 'authenticated' role. " +
                      "The registration process checks for email uniqueness within the project."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User successfully registered", 
                     content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input or email already exists",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody SignupRequest request) {
        return ResponseEntity.ok(authService.signup(request));
    }

    @Operation(
        summary = "Authenticate user",
        description = "Authenticates a user using email and password. Returns a JWT containing 'userId', 'projectRef', and 'role'. " +
                      "The JWT must be provided in the 'Authorization: Bearer <token>' header for subsequent requests to protected endpoints. " +
                      "Authentication is scoped to the provided 'projectRef'."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authentication successful", 
                     content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "User not found in the specified project",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
