package com.gdb.account.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdb.account.client.AuthClient; // Local package verified
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean; // Use @MockBean if on an older Spring Boot 3 version
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthClient authClient;

    static class AccountOnboardingDto {
        public String username;
        public String email;
        public String role;

        public AccountOnboardingDto(String username, String email, String role) {
            this.username = username;
            this.email = email;
            this.role = role;
        }
    }

    // ==========================================
    // WORKFLOW TESTS
    // ==========================================

    @Test
    @DisplayName("Should successfully onboard a new account when token is a valid ADMIN")
    void shouldOnboardAccountSuccessfully() throws Exception {
        // Arrange
        AccountOnboardingDto validAccount = new AccountOnboardingDto("johndoe", "john@example.com", "USER");
        String mockToken = "VALID_ADMIN_TOKEN";

        // Mock the token validation response with an ADMIN role
        AuthClient.TokenValidationResponse mockResponse = Mockito.mock(AuthClient.TokenValidationResponse.class);
        Mockito.when(mockResponse.isValid()).thenReturn(true);
        Mockito.when(mockResponse.getUserId()).thenReturn(1L);
        Mockito.when(mockResponse.getLoginId()).thenReturn("admin_user");
        Mockito.when(mockResponse.getRole()).thenReturn("ADMIN");

        Mockito.when(authClient.validateToken(mockToken)).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/accounts/onboard")
                        .header("Authorization", "Bearer " + mockToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAccount)))
                .andExpect(status().isCreated());
    }

    // ==========================================
    // PERMISSION / SECURITY TESTS
    // ==========================================

    @Test
    @DisplayName("Should return 403 Forbidden when token is valid but has role USER")
    void shouldDenyOnboardingForUnauthorizedRole() throws Exception {
        // Arrange
        AccountOnboardingDto validAccount = new AccountOnboardingDto("unauthorized", "test@example.com", "USER");
        String mockToken = "VALID_USER_TOKEN";

        // Mock response with a standard USER role instead of ADMIN
        AuthClient.TokenValidationResponse mockResponse = Mockito.mock(AuthClient.TokenValidationResponse.class);
        Mockito.when(mockResponse.isValid()).thenReturn(true);
        Mockito.when(mockResponse.getUserId()).thenReturn(2L);
        Mockito.when(mockResponse.getLoginId()).thenReturn("standard_user");
        Mockito.when(mockResponse.getRole()).thenReturn("USER");

        Mockito.when(authClient.validateToken(mockToken)).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(post("/api/accounts/onboard")
                        .header("Authorization", "Bearer " + mockToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAccount)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when no Authorization header is provided")
    void shouldDenyOnboardingForMissingHeader() throws Exception {
        // Arrange
        AccountOnboardingDto validAccount = new AccountOnboardingDto("anonymous", "anon@example.com", "USER");

        // Act & Assert
        mockMvc.perform(post("/api/accounts/onboard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAccount)))
                .andExpect(status().isUnauthorized());
    }
}