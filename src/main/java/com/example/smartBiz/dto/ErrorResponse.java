package com.example.smartBiz.dto;

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
@Schema(description = "Standard error response returned by all error handlers")
public class ErrorResponse {
    @Schema(description = "HTTP status code", example = "404")
    private int status;
    @Schema(description = "Human-readable error message", example = "Resource not found")
    private String message;
    @Schema(description = "Timestamp of the error")
    private LocalDateTime timestamp;
    @Schema(description = "Request URI that caused the error", example = "/v1/api/invoices/999")
    private String path;
}
