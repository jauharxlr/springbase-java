package com.jauharxlr.springbase.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Response containing the authentication token")
public class AuthResponse {
    @Schema(description = "JWT Access Token used for authorized requests", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;
    
    @Schema(description = "The type of the token", example = "Bearer")
    private String tokenType = "Bearer";
    
    public AuthResponse(String accessToken) {
        this.accessToken = accessToken;
    }
}
