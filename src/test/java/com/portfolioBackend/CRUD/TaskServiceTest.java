package com.portfolioBackend.CRUD;

import com.portfolioBackend.auth.user.User;
import com.portfolioBackend.auth.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private TaskService taskService;

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void createAssignsOwnerTrimsTitleAndPersistsTask() {
        User owner = userWithId(7L, "oscar", "oscar@test.com");
        when(userService.requireById(7L)).thenReturn(owner);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task created = taskService.create(7L, "  Preparar demo de Swagger para CRUD  ");

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());

        assertAll(
                () -> assertSame(owner, created.getUser()),
                () -> assertEquals("Preparar demo de Swagger para CRUD", created.getTitle()),
                () -> assertFalse(created.isCompleted()),
                () -> assertSame(owner, captor.getValue().getUser()),
                () -> assertEquals("Preparar demo de Swagger para CRUD", captor.getValue().getTitle())
        );
    }

    @Test
    void createRejectsBlankTitleBeforeLoadingUser() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> taskService.create(7L, "   "));

        assertEquals("VALIDATION_ERROR", ex.getMessage());
        verify(userService, never()).requireById(any());
        verify(taskRepository, never()).save(any(Task.class));
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void updateRenamesTitleWhenCalledByOwner() {
        User owner = userWithId(3L, "alice", "alice@test.com");
        // isAdmin checks requireById when username is not "oscar"
        when(userService.requireById(3L)).thenReturn(owner);
        Task task = taskOwnedBy(owner);
        when(taskRepository.findById(42L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task updated = taskService.update(3L, "alice", 42L, "  Nuevo titulo  ", null);

        assertEquals("Nuevo titulo", updated.getTitle());
        assertFalse(updated.isCompleted());
    }

    @Test
    void updateRejectsBlankTitleOnRename() {
        User owner = userWithId(3L, "alice", "alice@test.com");
        when(userService.requireById(3L)).thenReturn(owner);
        Task task = taskOwnedBy(owner);
        when(taskRepository.findById(42L)).thenReturn(Optional.of(task));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> taskService.update(3L, "alice", 42L, "   ", null));

        assertEquals("VALIDATION_ERROR", ex.getMessage());
    }

    @Test
    void updateForbidsRenamingOtherUsersTask() {
        User owner = userWithId(3L, "alice", "alice@test.com");
        User stranger = userWithId(9L, "bob", "bob@test.com");
        Task task = taskOwnedBy(owner);
        when(taskRepository.findById(42L)).thenReturn(Optional.of(task));
        // isAdmin resolves stranger via requireById since "bob" != "oscar"
        when(userService.requireById(9L)).thenReturn(stranger);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> taskService.update(9L, "bob", 42L, "Titulo robado", null));

        assertEquals("FORBIDDEN", ex.getMessage());
    }

    @Test
    void updateAllowsAdminToRenameAnyTask() {
        // "oscar" matches ADMIN_USERNAME, so isAdmin returns true without calling requireById
        User owner = userWithId(3L, "alice", "alice@test.com");
        Task task = taskOwnedBy(owner);
        when(taskRepository.findById(42L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task updated = taskService.update(1L, "oscar", 42L, "Titulo cambiado por admin", null);

        assertEquals("Titulo cambiado por admin", updated.getTitle());
    }

    @Test
    void updateCompletesTaskAndRecordsCompletedByUser() {
        User owner = userWithId(3L, "alice", "alice@test.com");
        User completer = userWithId(9L, "bob", "bob@test.com");
        Task task = taskOwnedBy(owner);
        when(taskRepository.findById(42L)).thenReturn(Optional.of(task));
        // isAdmin for "bob" + applyCompletion both use requireById(9L)
        when(userService.requireById(9L)).thenReturn(completer);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task updated = taskService.update(9L, "bob", 42L, null, true);

        assertTrue(updated.isCompleted());
        assertSame(completer, updated.getCompletedBy());
    }

    @Test
    void updateUncompletesClearsCompletedBy() {
        User owner = userWithId(3L, "alice", "alice@test.com");
        when(userService.requireById(3L)).thenReturn(owner);
        Task task = taskOwnedBy(owner);
        task.setCompleted(true);
        task.setCompletedBy(owner);
        when(taskRepository.findById(42L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task updated = taskService.update(3L, "alice", 42L, null, false);

        assertFalse(updated.isCompleted());
        assertNull(updated.getCompletedBy());
    }

    @Test
    void updateThrowsWhenTaskNotFound() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> taskService.update(3L, "alice", 99L, "X", null));

        assertEquals("TASK_NOT_FOUND", ex.getMessage());
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void deleteRemovesTaskWhenCalledByOwner() {
        User owner = userWithId(3L, "alice", "alice@test.com");
        when(userService.requireById(3L)).thenReturn(owner);
        Task task = taskOwnedBy(owner);
        when(taskRepository.findById(42L)).thenReturn(Optional.of(task));

        taskService.delete(3L, "alice", 42L);

        verify(taskRepository).delete(task);
    }

    @Test
    void deleteForbidsNonOwnerNonAdmin() {
        User owner = userWithId(3L, "alice", "alice@test.com");
        User stranger = userWithId(9L, "bob", "bob@test.com");
        Task task = taskOwnedBy(owner);
        when(taskRepository.findById(42L)).thenReturn(Optional.of(task));
        when(userService.requireById(9L)).thenReturn(stranger);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> taskService.delete(9L, "bob", 42L));

        assertEquals("FORBIDDEN", ex.getMessage());
        verify(taskRepository, never()).delete(any());
    }

    @Test
    void deleteAllowsAdminToDeleteAnyTask() {
        // "oscar" matches ADMIN_USERNAME — isAdmin returns true without calling requireById
        User owner = userWithId(3L, "alice", "alice@test.com");
        Task task = taskOwnedBy(owner);
        when(taskRepository.findById(42L)).thenReturn(Optional.of(task));

        taskService.delete(1L, "oscar", 42L);

        verify(taskRepository).delete(task);
    }

    @Test
    void deleteThrowsWhenTaskNotFound() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> taskService.delete(3L, "alice", 99L));

        assertEquals("TASK_NOT_FOUND", ex.getMessage());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private User userWithId(Long id, String username, String email) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        return user;
    }

    private Task taskOwnedBy(User owner) {
        Task task = new Task();
        task.setUser(owner);
        task.setTitle("Tarea de prueba");
        return task;
    }
}
