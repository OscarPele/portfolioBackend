package com.portfolioBackend.CRUD;

import com.portfolioBackend.CRUD.dto.ApiErrorResponse;
import com.portfolioBackend.CRUD.dto.CreateTaskRequest;
import com.portfolioBackend.CRUD.dto.TaskResponse;
import com.portfolioBackend.CRUD.dto.TaskUserResponse;
import com.portfolioBackend.CRUD.dto.UpdateTaskRequest;
import com.portfolioBackend.security.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tasks")
@Tag(name = "Tasks", description = "CRUD autenticado de tareas.")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    @Operation(
            summary = "Listar tareas",
            description = """
                    Devuelve todas las tareas ordenadas por fecha de creacion descendente.
                    Cada tarea incluye flags `canModify` y `canToggleComplete` calculados para
                    el usuario autenticado, por lo que la misma lista se adapta al rol del llamante.
                    Un usuario normal solo puede modificar sus propias tareas; el admin puede modificar cualquiera.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de tareas (puede estar vacia).",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = TaskResponse.class)),
                            examples = @ExampleObject(
                                    name = "TaskList",
                                    value = """
                                            [
                                              {
                                                "id": 12,
                                                "title": "Preparar demo de Swagger para CRUD",
                                                "completed": false,
                                                "createdAt": "2026-04-04T15:30:00Z",
                                                "updatedAt": "2026-04-04T15:30:00Z",
                                                "canModify": true,
                                                "canToggleComplete": true,
                                                "owner": { "id": 7, "username": "oscar" },
                                                "completedBy": null
                                              },
                                              {
                                                "id": 11,
                                                "title": "Revisar pipeline de CI/CD",
                                                "completed": true,
                                                "createdAt": "2026-04-03T10:00:00Z",
                                                "updatedAt": "2026-04-03T11:45:00Z",
                                                "canModify": false,
                                                "canToggleComplete": false,
                                                "owner": { "id": 3, "username": "alice" },
                                                "completedBy": { "id": 3, "username": "alice" }
                                              }
                                            ]
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "No se ha enviado un JWT valido.")
    })
    public ResponseEntity<List<TaskResponse>> getAll(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        Long uid = JwtUtils.getUid(jwt);
        String username = jwt != null ? jwt.getSubject() : null;
        boolean isAdmin = taskService.isAdmin(uid, username);
        return ResponseEntity.ok(
                taskService.getAll().stream().map(task -> toDto(task, uid, isAdmin)).toList()
        );
    }

    @PostMapping
    @Operation(
            summary = "Crear tarea",
            description = "Crea una nueva tarea asociada al usuario autenticado. El titulo se normaliza con trim antes de guardarse."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Tarea creada correctamente.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TaskResponse.class),
                            examples = @ExampleObject(
                                    name = "TaskCreated",
                                    value = """
                                            {
                                              "id": 12,
                                              "title": "Preparar demo de Swagger para CRUD",
                                              "completed": false,
                                              "createdAt": "2026-04-04T15:30:00Z",
                                              "updatedAt": "2026-04-04T15:30:00Z",
                                              "canModify": true,
                                              "canToggleComplete": true,
                                              "owner": { "id": 7, "username": "oscar" },
                                              "completedBy": null
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "El titulo es nulo, vacio o solo contiene espacios.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\":\"VALIDATION_ERROR\"}")
                    )
            ),
            @ApiResponse(responseCode = "401", description = "No se ha enviado un JWT valido."),
            @ApiResponse(
                    responseCode = "404",
                    description = "El usuario autenticado no existe en base de datos.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\":\"USER_NOT_FOUND\"}")
                    )
            )
    })
    public ResponseEntity<TaskResponse> create(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Payload minimo para crear una tarea.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CreateTaskRequest.class),
                            examples = @ExampleObject(
                                    name = "CreateTaskRequest",
                                    value = "{\"title\": \"Preparar demo de Swagger para CRUD\"}"
                            )
                    )
            )
            @RequestBody CreateTaskRequest body) {
        Long uid = JwtUtils.getUid(jwt);
        String username = jwt != null ? jwt.getSubject() : null;
        Task createdTask = taskService.create(uid, body.title());
        boolean isAdmin = taskService.isAdmin(uid, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(createdTask, uid, isAdmin));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Actualizar tarea",
            description = """
                    Actualiza titulo y/o estado de completado de una tarea existente.
                    Ambos campos son opcionales: se puede enviar solo `title`, solo `completed`, o ambos.
                    Reglas de autorizacion:
                    - Cambiar `title`: solo el propietario o el admin.
                    - Cambiar `completed`: cualquier usuario autenticado puede completar una tarea ajena;
                      desmarcarla solo la puede hacer el propietario, el admin o quien la marco como completada.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Tarea actualizada correctamente.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TaskResponse.class),
                            examples = @ExampleObject(
                                    name = "TaskUpdated",
                                    value = """
                                            {
                                              "id": 12,
                                              "title": "Preparar demo final de Swagger",
                                              "completed": true,
                                              "createdAt": "2026-04-04T15:30:00Z",
                                              "updatedAt": "2026-04-04T16:00:00Z",
                                              "canModify": true,
                                              "canToggleComplete": true,
                                              "owner": { "id": 7, "username": "oscar" },
                                              "completedBy": { "id": 7, "username": "oscar" }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "El nuevo titulo es vacio o solo contiene espacios.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\":\"VALIDATION_ERROR\"}")
                    )
            ),
            @ApiResponse(responseCode = "401", description = "No se ha enviado un JWT valido."),
            @ApiResponse(
                    responseCode = "403",
                    description = "El usuario no tiene permiso para realizar esta operacion sobre la tarea.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\":\"FORBIDDEN\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No existe ninguna tarea con el id proporcionado.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\":\"TASK_NOT_FOUND\"}")
                    )
            )
    })
    public ResponseEntity<TaskResponse> update(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Identificador de la tarea.", example = "12")
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Campos a actualizar. Ambos son opcionales.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UpdateTaskRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "RenameOnly",
                                            summary = "Solo renombrar",
                                            value = "{\"title\": \"Preparar demo final de Swagger\"}"
                                    ),
                                    @ExampleObject(
                                            name = "CompleteOnly",
                                            summary = "Solo completar",
                                            value = "{\"completed\": true}"
                                    ),
                                    @ExampleObject(
                                            name = "RenameAndComplete",
                                            summary = "Renombrar y completar",
                                            value = "{\"title\": \"Preparar demo final de Swagger\", \"completed\": true}"
                                    )
                            }
                    )
            )
            @RequestBody UpdateTaskRequest body) {
        Long uid = JwtUtils.getUid(jwt);
        String username = jwt != null ? jwt.getSubject() : null;
        boolean isAdmin = taskService.isAdmin(uid, username);
        Task updated = taskService.update(uid, username, id, body.title(), body.completed());
        return ResponseEntity.ok(toDto(updated, uid, isAdmin));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Eliminar tarea",
            description = """
                    Elimina permanentemente una tarea. Solo el propietario de la tarea o el admin
                    pueden borrarla. Devuelve 204 sin cuerpo si la operacion es exitosa.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Tarea eliminada correctamente."),
            @ApiResponse(responseCode = "401", description = "No se ha enviado un JWT valido."),
            @ApiResponse(
                    responseCode = "403",
                    description = "El usuario no es el propietario ni el admin.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\":\"FORBIDDEN\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No existe ninguna tarea con el id proporcionado.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\":\"TASK_NOT_FOUND\"}")
                    )
            )
    })
    public ResponseEntity<Void> delete(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Identificador de la tarea.", example = "12")
            @PathVariable Long id) {
        Long uid = JwtUtils.getUid(jwt);
        String username = jwt != null ? jwt.getSubject() : null;
        taskService.delete(uid, username, id);
        return ResponseEntity.noContent().build();
    }

    private TaskResponse toDto(Task task, Long uid, boolean isAdmin) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.isCompleted(),
                task.getCreatedAt().toString(),
                task.getUpdatedAt().toString(),
                taskService.canModify(uid, isAdmin, task),
                taskService.canToggleComplete(uid, isAdmin, task),
                toUserDto(task.getUser()),
                task.getCompletedBy() != null ? toUserDto(task.getCompletedBy()) : null
        );
    }

    private TaskUserResponse toUserDto(com.portfolioBackend.auth.user.User user) {
        return new TaskUserResponse(user.getId(), user.getUsername());
    }
}