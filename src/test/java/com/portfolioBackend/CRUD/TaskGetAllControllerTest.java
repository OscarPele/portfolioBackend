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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskGetAllControllerTest {

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
    void getAllReturnsEmptyListWhenNoTasks() throws Exception {
        User user = saveEnabledUser("reader", "reader@test.com");

        mockMvc.perform(get("/tasks").with(jwtFor(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getAllReturnsAllTasksWithCorrectFields() throws Exception {
        User owner = saveEnabledUser("alice", "alice@test.com");
        User viewer = saveEnabledUser("bob", "bob@test.com");

        Task t = new Task();
        t.setUser(owner);
        t.setTitle("Tarea de alice");
        taskRepository.save(t);

        mockMvc.perform(get("/tasks").with(jwtFor(viewer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Tarea de alice"))
                .andExpect(jsonPath("$[0].completed").value(false))
                .andExpect(jsonPath("$[0].owner.username").value("alice"))
                // bob no es el owner, no puede modificar
                .andExpect(jsonPath("$[0].canModify").value(false))
                // cualquier usuario puede completar una tarea ajena
                .andExpect(jsonPath("$[0].canToggleComplete").value(true));
    }

    @Test
    void getAllReturnsCanModifyTrueForOwner() throws Exception {
        User owner = saveEnabledUser("alice", "alice@test.com");

        Task t = new Task();
        t.setUser(owner);
        t.setTitle("Mi tarea");
        taskRepository.save(t);

        mockMvc.perform(get("/tasks").with(jwtFor(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].canModify").value(true))
                .andExpect(jsonPath("$[0].canToggleComplete").value(true));
    }

    @Test
    void getAllReturnsUnauthorizedWithoutJwt() throws Exception {
        mockMvc.perform(get("/tasks"))
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
