package com.jauharxlr.springbase.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Response containing the authentication token and user info")
public class AuthResponse {
    @Schema(description = "JWT Access Token used for authorized requests", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;
    
    @Schema(description = "The type of the token", example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "The unique identifier of the user")
    private UUID userId;

    @Schema(description = "The email address of the user")
    private String email;
    
    public AuthResponse(String accessToken) {
        this.accessToken = accessToken;
    }
}
