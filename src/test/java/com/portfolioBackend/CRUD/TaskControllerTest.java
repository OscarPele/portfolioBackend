package com.portfolioBackend.CRUD;

import com.portfolioBackend.auth.user.User;
import com.portfolioBackend.auth.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void getTasksMarksOnlyOwnerAsModifiable() throws Exception {
        User owner = saveEnabledUser("owner", "owner@test.com");
        User other = saveEnabledUser("other", "other@test.com");
        saveTask(owner, "Task creada por owner");

        mockMvc.perform(get("/tasks").with(jwtFor(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].owner.id").value(owner.getId()))
                .andExpect(jsonPath("$[0].owner.username").value(owner.getUsername()))
                .andExpect(jsonPath("$[0].canModify").value(true))
                .andExpect(jsonPath("$[0].canToggleComplete").value(true))
                .andExpect(jsonPath("$[0].completedBy").value(nullValue()));

        mockMvc.perform(get("/tasks").with(jwtFor(other)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].canModify").value(false))
                .andExpect(jsonPath("$[0].canToggleComplete").value(true))
                .andExpect(jsonPath("$[0].completedBy").value(nullValue()));
    }

    @Test
    void nonOwnerCannotEditOrDeleteTask() throws Exception {
        User owner = saveEnabledUser("owner", "owner@test.com");
        User other = saveEnabledUser("other", "other@test.com");
        Task task = saveTask(owner, "Task protegida");

        mockMvc.perform(put("/tasks/{id}", task.getId())
                        .with(jwtFor(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Cambio no permitido\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/tasks/{id}", task.getId()).with(jwtFor(other)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminByUsernameCanUpdateAndDeleteAnyTask() throws Exception {
        User owner = saveEnabledUser("owner", "owner@test.com");
        User admin = saveEnabledUser("oscar", "admin@test.com");
        Task task = saveTask(owner, "Task del owner");

        mockMvc.perform(put("/tasks/{id}", task.getId())
                        .with(jwtFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Task editada por admin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Task editada por admin"))
                .andExpect(jsonPath("$.canModify").value(true))
                .andExpect(jsonPath("$.canToggleComplete").value(true));

        mockMvc.perform(delete("/tasks/{id}", task.getId()).with(jwtFor(admin)))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminBySeedEmailSeesAllTasksAsModifiable() throws Exception {
        User owner = saveEnabledUser("owner", "owner@test.com");
        User admin = saveEnabledUser("seed-admin", "oscar@seed.local");
        saveTask(owner, "Task visible para admin");

        mockMvc.perform(get("/tasks").with(jwtFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].canModify").value(true))
                .andExpect(jsonPath("$[0].canToggleComplete").value(true));
    }

    @Test
    void otherUserCanCompleteTaskAndBecomeCompleter() throws Exception {
        User owner = saveEnabledUser("owner", "owner@test.com");
        User other = saveEnabledUser("other", "other@test.com");
        Task task = saveTask(owner, "Task colaborativa");

        mockMvc.perform(put("/tasks/{id}", task.getId())
                        .with(jwtFor(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(task.getId()))
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.canModify").value(false))
                .andExpect(jsonPath("$.canToggleComplete").value(true))
                .andExpect(jsonPath("$.completedBy.id").value(other.getId()))
                .andExpect(jsonPath("$.completedBy.username").value(other.getUsername()));
    }

    @Test
    void thirdUserCannotUndoTaskCompletedBySomeoneElse() throws Exception {
        User owner = saveEnabledUser("owner", "owner@test.com");
        User completer = saveEnabledUser("completer", "completer@test.com");
        User outsider = saveEnabledUser("outsider", "outsider@test.com");
        Task task = saveTask(owner, "Task ya completada");

        mockMvc.perform(put("/tasks/{id}", task.getId())
                        .with(jwtFor(completer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/tasks/{id}", task.getId())
                        .with(jwtFor(outsider))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanUpdateAndDeleteOwnTask() throws Exception {
        User owner = saveEnabledUser("owner", "owner@test.com");
        Task task = saveTask(owner, "Task original");

        mockMvc.perform(put("/tasks/{id}", task.getId())
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Task editada\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(task.getId()))
                .andExpect(jsonPath("$.title").value("Task editada"))
                .andExpect(jsonPath("$.canModify").value(true));

        mockMvc.perform(delete("/tasks/{id}", task.getId()).with(jwtFor(owner)))
                .andExpect(status().isNoContent());
    }

    @Test
    void ownerCanUndoTaskCompletedByAnotherUser() throws Exception {
        User owner = saveEnabledUser("owner", "owner@test.com");
        User other = saveEnabledUser("other", "other@test.com");
        Task task = saveTask(owner, "Task reversible");

        mockMvc.perform(put("/tasks/{id}", task.getId())
                        .with(jwtFor(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/tasks/{id}", task.getId())
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.completedBy").value(nullValue()))
                .andExpect(jsonPath("$.canModify").value(true))
                .andExpect(jsonPath("$.canToggleComplete").value(true));
    }

    private User saveEnabledUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setEnabled(true);
        return userRepository.save(user);
    }

    private Task saveTask(User owner, String title) {
        Task task = new Task();
        task.setUser(owner);
        task.setTitle(title);
        return taskRepository.save(task);
    }

    private JwtRequestPostProcessor jwtFor(User user) {
        return jwt().jwt(jwt -> jwt.subject(user.getUsername()).claim("uid", user.getId()));
    }
}
