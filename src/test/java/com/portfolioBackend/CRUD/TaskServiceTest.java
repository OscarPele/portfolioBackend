package com.portfolioBackend.CRUD;

import com.portfolioBackend.auth.user.User;
import com.portfolioBackend.auth.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void createAssignsOwnerTrimsTitleAndPersistsTask() {
        User owner = new User();
        owner.setId(7L);
        owner.setUsername("oscar");
        owner.setEmail("oscar@test.com");

        when(userService.requireById(7L)).thenReturn(owner);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task createdTask = taskService.create(7L, "  Preparar demo de Swagger para CRUD  ");

        ArgumentCaptor<Task> savedTaskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(savedTaskCaptor.capture());

        assertAll(
                () -> assertSame(owner, createdTask.getUser()),
                () -> assertEquals("Preparar demo de Swagger para CRUD", createdTask.getTitle()),
                () -> assertFalse(createdTask.isCompleted()),
                () -> assertSame(owner, savedTaskCaptor.getValue().getUser()),
                () -> assertEquals("Preparar demo de Swagger para CRUD", savedTaskCaptor.getValue().getTitle())
        );
    }

    @Test
    void createRejectsBlankTitleBeforeLoadingUser() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> taskService.create(7L, "   "));

        assertEquals("VALIDATION_ERROR", ex.getMessage());
        verify(userService, never()).requireById(any());
        verify(taskRepository, never()).save(any(Task.class));
    }
}
