package com.portfolioBackend.CRUD;

import com.portfolioBackend.CRUD.dto.ApiErrorResponse;
import com.portfolioBackend.CRUD.dto.CreateTaskRequest;
import com.portfolioBackend.CRUD.dto.TaskResponse;
import com.portfolioBackend.CRUD.dto.TaskUserResponse;
import com.portfolioBackend.security.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import java.util.Map;

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
    public ResponseEntity<List<TaskResponse>> getAll(@AuthenticationPrincipal Jwt jwt) {
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
                                              "owner": {
                                                "id": 7,
                                                "username": "oscar"
                                              },
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
            @ApiResponse(
                    responseCode = "401",
                    description = "No se ha enviado un JWT valido."
            ),
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
                                    value = """
                                            {
                                              "title": "Preparar demo de Swagger para CRUD"
                                            }
                                            """
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
    public ResponseEntity<TaskResponse> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Long uid = JwtUtils.getUid(jwt);
        String username = jwt != null ? jwt.getSubject() : null;
        String title = body.containsKey("title") ? (String) body.get("title") : null;
        Boolean completed = body.containsKey("completed") ? (Boolean) body.get("completed") : null;
        boolean isAdmin = taskService.isAdmin(uid, username);
        return ResponseEntity.ok(toDto(taskService.update(uid, username, id, title, completed), uid, isAdmin));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
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
