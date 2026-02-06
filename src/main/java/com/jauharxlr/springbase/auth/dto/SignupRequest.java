package com.jauharxlr.springbase.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Information required to create a new user account")
public class SignupRequest {
    @Schema(description = "User's email address for registration", example = "newuser@example.com")
    private String email;
    
    @Schema(description = "Secure password for the new account", example = "strongpassword!99")
    private String password;
    
    @Schema(description = "Unique reference for the project/tenant the user belongs to", example = "my-awesome-project")
    private String projectRef;
}
