package com.portfolioBackend.CRUD.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record UpdateTaskRequest(
        @Size(max = 255)
        @Schema(
                description = "Nuevo titulo de la tarea. Si se omite, el titulo no cambia.",
                example = "Preparar demo final de Swagger",
                nullable = true
        )
        String title,

        @Schema(
                description = "Nuevo estado de completado. Si se omite, el estado no cambia.",
                example = "true",
                nullable = true
        )
        Boolean completed
) {
}
