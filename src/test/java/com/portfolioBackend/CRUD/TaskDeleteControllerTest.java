package com.portfolioBackend.CRUD;

import com.portfolioBackend.auth.user.User;
import com.portfolioBackend.auth.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskDeleteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User owner;
    private User stranger;
    private Task task;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        userRepository.deleteAll();

        owner = saveEnabledUser("alice", "alice@test.com");
        stranger = saveEnabledUser("bob", "bob@test.com");

        task = new Task();
        task.setUser(owner);
        task.setTitle("Tarea a eliminar");
        task = taskRepository.save(task);
    }

    @Test
    void deleteReturnsNoContentAndRemovesTask() throws Exception {
        mockMvc.perform(delete("/tasks/" + task.getId()).with(jwtFor(owner)))
                .andExpect(status().isNoContent());

        assertFalse(taskRepository.existsById(task.getId()));
    }

    @Test
    void deleteForbidsNonOwner() throws Exception {
        mockMvc.perform(delete("/tasks/" + task.getId()).with(jwtFor(stranger)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void deleteReturnsNotFoundForMissingTask() throws Exception {
        mockMvc.perform(delete("/tasks/99999").with(jwtFor(owner)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));
    }

    @Test
    void deleteReturnsUnauthorizedWithoutJwt() throws Exception {
        mockMvc.perform(delete("/tasks/" + task.getId()))
                .andExpect(status().isUnauthorized());
    }

    private User saveEnabledUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setEnabled(true);
        return userRepository.save(user);
    }

    private JwtRequestPostProcessor jwtFor(User user) {
        return jwt().jwt(j -> j.subject(user.getUsername()).claim("uid", user.getId()));
    }
}
