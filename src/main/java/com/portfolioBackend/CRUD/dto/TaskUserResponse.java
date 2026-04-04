package com.portfolioBackend.CRUD.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TaskUserResponse(
        @Schema(description = "Identificador del usuario.", example = "7")
        Long id,
        @Schema(description = "Nombre de usuario.", example = "oscar")
        String username
) {
}
