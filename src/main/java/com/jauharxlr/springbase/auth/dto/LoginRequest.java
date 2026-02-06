package com.jauharxlr.springbase.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Credentials for user authentication")
public class LoginRequest {
    @Schema(description = "User's registered email address", example = "user@example.com")
    private String email;
    
    @Schema(description = "User's password", example = "securepassword123")
    private String password;
    
    @Schema(description = "Unique reference for the project/tenant", example = "my-awesome-project")
    private String projectRef;
}
