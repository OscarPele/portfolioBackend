package com.portfolioBackend.CRUD.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ApiErrorResponse(
        @Schema(description = "Codigo funcional del error.", example = "VALIDATION_ERROR")
        String code
) {
}
