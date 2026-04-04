package com.portfolioBackend.CRUD;

import com.portfolioBackend.auth.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskService {

    private static final String ADMIN_USERNAME = "oscar";
    private static final String ADMIN_EMAIL = "oscar@seed.local";

    private final TaskRepository taskRepo;
    private final UserService userService;

    public TaskService(TaskRepository taskRepo, UserService userService) {
        this.taskRepo = taskRepo;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<Task> getAll() {
        return taskRepo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Task create(Long userId, String title) {
        String normalizedTitle = normalizeTitle(title);
        var user = userService.requireById(userId);
        var task = new Task();
        task.setUser(user);
        task.setTitle(normalizedTitle);
        return taskRepo.save(task);
    }

    @Transactional
    public Task update(Long userId, String username, Long taskId, String title, Boolean completed) {
        var task = taskRepo.findById(taskId)
                .orElseThrow(() -> new RuntimeException("TASK_NOT_FOUND"));
        boolean isAdmin = isAdmin(userId, username);

        if (title != null) {
            if (!canModify(userId, isAdmin, task)) {
                throw new RuntimeException("FORBIDDEN");
            }
            task.setTitle(normalizeTitle(title));
        }

        if (completed != null) {
            if (!canToggleComplete(userId, isAdmin, task)) {
                throw new RuntimeException("FORBIDDEN");
            }
            applyCompletion(userId, task, completed);
        }

        return taskRepo.save(task);
    }

    @Transactional
    public void delete(Long userId, String username, Long taskId) {
        var task = taskRepo.findById(taskId)
                .orElseThrow(() -> new RuntimeException("TASK_NOT_FOUND"));
        if (!canModify(userId, isAdmin(userId, username), task)) {
            throw new RuntimeException("FORBIDDEN");
        }
        taskRepo.delete(task);
    }

    boolean isAdmin(Long userId, String username) {
        if (ADMIN_USERNAME.equalsIgnoreCase(username)) {
            return true;
        }
        if (userId == null) {
            return false;
        }
        var actor = userService.requireById(userId);
        return ADMIN_USERNAME.equalsIgnoreCase(actor.getUsername())
                || ADMIN_EMAIL.equalsIgnoreCase(actor.getEmail());
    }

    boolean canModify(Long userId, boolean isAdmin, Task task) {
        if (task == null || task.getUser() == null || task.getUser().getId() == null) {
            return false;
        }
        return isAdmin || task.getUser().getId().equals(userId);
    }

    boolean canToggleComplete(Long userId, boolean isAdmin, Task task) {
        if (userId == null || task == null) {
            return false;
        }
        if (isAdmin) {
            return true;
        }
        if (!task.isCompleted()) {
            return true;
        }
        if (canModify(userId, false, task)) {
            return true;
        }
        return task.getCompletedBy() != null && userId.equals(task.getCompletedBy().getId());
    }

    private void applyCompletion(Long userId, Task task, boolean completed) {
        task.setCompleted(completed);
        if (completed) {
            task.setCompletedBy(userService.requireById(userId));
            return;
        }
        task.setCompletedBy(null);
    }

    private String normalizeTitle(String title) {
        String trimmedTitle = title == null ? "" : title.trim();
        if (trimmedTitle.isEmpty()) {
            throw new RuntimeException("VALIDATION_ERROR");
        }
        return trimmedTitle;
    }
}
