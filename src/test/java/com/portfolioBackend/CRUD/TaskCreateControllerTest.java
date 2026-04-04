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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskCreateControllerTest {

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
    void createTaskReturnsCreatedTaskForAuthenticatedOwner() throws Exception {
        User owner = saveEnabledUser("creator", "creator@test.com");

        mockMvc.perform(post("/tasks")
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"  Preparar demo de Swagger para CRUD  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Preparar demo de Swagger para CRUD"))
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.owner.id").value(owner.getId()))
                .andExpect(jsonPath("$.owner.username").value(owner.getUsername()))
                .andExpect(jsonPath("$.canModify").value(true))
                .andExpect(jsonPath("$.canToggleComplete").value(true))
                .andExpect(jsonPath("$.completedBy").value(nullValue()));
    }

    @Test
    void createTaskRejectsBlankTitle() throws Exception {
        User owner = saveEnabledUser("creator", "creator@test.com");

        mockMvc.perform(post("/tasks")
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
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
        return jwt().jwt(jwt -> jwt.subject(user.getUsername()).claim("uid", user.getId()));
    }
}
