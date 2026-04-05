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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskUpdateControllerTest {

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
        task.setTitle("Titulo original");
        task = taskRepository.save(task);
    }

    @Test
    void updateRenamesTitleWhenCalledByOwner() throws Exception {
        mockMvc.perform(put("/tasks/" + task.getId())
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"  Titulo nuevo  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Titulo nuevo"))
                .andExpect(jsonPath("$.completed").value(false));
    }

    @Test
    void updateRejectsBlankTitle() throws Exception {
        mockMvc.perform(put("/tasks/" + task.getId())
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void updateForbidsRenamingOtherUsersTask() throws Exception {
        mockMvc.perform(put("/tasks/" + task.getId())
                        .with(jwtFor(stranger))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Titulo robado\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void updateCompletesTaskByAnyAuthenticatedUser() throws Exception {
        mockMvc.perform(put("/tasks/" + task.getId())
                        .with(jwtFor(stranger))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.completedBy.username").value(stranger.getUsername()));
    }

    @Test
    void updateUncompletesTaskByOwner() throws Exception {
        // First complete the task as owner
        mockMvc.perform(put("/tasks/" + task.getId())
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\": true}"))
                .andExpect(status().isOk());

        // Then uncomplete it
        mockMvc.perform(put("/tasks/" + task.getId())
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.completedBy").value(nullValue()));
    }

    @Test
    void updateReturnsNotFoundForMissingTask() throws Exception {
        mockMvc.perform(put("/tasks/99999")
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"X\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));
    }

    @Test
    void updateReturnsUnauthorizedWithoutJwt() throws Exception {
        mockMvc.perform(put("/tasks/" + task.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"X\"}"))
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
