package com.portfolioBackend.CRUD.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(
        @NotBlank
        @Size(max = 255)
        @Schema(
                description = "Titulo visible de la tarea.",
                example = "Preparar demo de Swagger para CRUD",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String title
) {
}
