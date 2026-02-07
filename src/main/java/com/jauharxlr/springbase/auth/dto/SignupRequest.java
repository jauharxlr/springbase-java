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

    @Schema(description = "Optional company ID to associate with the user", example = "550e8400-e29b-41d4-a716-446655440000")
    private java.util.UUID companyId;

    @Schema(description = "Optional tenant ID to associate with the user", example = "550e8400-e29b-41d4-a716-446655440001")
    private java.util.UUID tenantId;

    @Schema(description = "Optional metadata for compatibility with Supabase client options")
    private Options options;

    @Data
    public static class Options {
        private java.util.Map<String, Object> data;
    }
}
