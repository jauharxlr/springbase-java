package com.jauharxlr.springbase.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standard error response structure")
public class ErrorResponse {
    
    @Schema(description = "HTTP status code", example = "400")
    private int status;
    
    @Schema(description = "Short error type/name", example = "Bad Request")
    private String error;
    
    @Schema(description = "Detailed error message", example = "The email address is already registered.")
    private String message;
    
    @Schema(description = "Path where the error occurred", example = "/auth/v1/signup")
    private String path;
    
    @Schema(description = "Timestamp of the error")
    private LocalDateTime timestamp;
}
