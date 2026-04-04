package com.portfolioBackend.CRUD.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TaskResponse(
        @Schema(description = "Identificador de la tarea.", example = "12")
        Long id,
        @Schema(description = "Titulo normalizado de la tarea.", example = "Preparar demo de Swagger para CRUD")
        String title,
        @Schema(description = "Estado de completado de la tarea.", example = "false")
        boolean completed,
        @Schema(description = "Fecha de creacion en formato ISO-8601.", example = "2026-04-04T15:30:00Z")
        String createdAt,
        @Schema(description = "Fecha de ultima actualizacion en formato ISO-8601.", example = "2026-04-04T15:30:00Z")
        String updatedAt,
        @Schema(description = "Indica si el usuario autenticado puede editar o borrar la tarea.", example = "true")
        boolean canModify,
        @Schema(description = "Indica si el usuario autenticado puede cambiar el estado de completado.", example = "true")
        boolean canToggleComplete,
        @Schema(description = "Propietario de la tarea.")
        TaskUserResponse owner,
        @Schema(description = "Usuario que completo la tarea. Es null si aun no esta completada.", nullable = true)
        TaskUserResponse completedBy
) {
}
