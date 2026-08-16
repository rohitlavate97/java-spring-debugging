package com.eopis.security;

import com.eopis.security.dto.AuthRequest;
import com.eopis.security.entity.Role;
import com.eopis.security.entity.User;
import com.eopis.security.repository.RoleRepository;
import com.eopis.security.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        Role adminRole = roleRepository.save(new Role("ADMIN", "Administrator"));
        Role userRole = roleRepository.save(new Role("USER", "Standard User"));

        User admin = new User("admin_test", "admin@eopis.local", passwordEncoder.encode("Password123!"));
        admin.setRoles(Set.of(adminRole));
        userRepository.save(admin);

        User normalUser = new User("john_doe", "john@eopis.local", passwordEncoder.encode("Password123!"));
        normalUser.setRoles(Set.of(userRole));
        userRepository.save(normalUser);
    }

    @Test
    @DisplayName("Public actuator endpoints are accessible without authentication")
    void shouldAllowUnauthenticatedAccessToActuator() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Successful login returns JWT token")
    void shouldAuthenticateAndReturnJwt() throws Exception {
        AuthRequest loginRequest = new AuthRequest("john_doe", "Password123!");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        assertNotNull(responseJson);
    }

    @Test
    @DisplayName("Invalid credentials return 401/403 unauthorized")
    void shouldRejectInvalidCredentials() throws Exception {
        AuthRequest badRequest = new AuthRequest("john_doe", "WrongPassword!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isForbidden());
    }
}
