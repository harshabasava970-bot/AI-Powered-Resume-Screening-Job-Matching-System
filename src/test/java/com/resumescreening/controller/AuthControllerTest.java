package com.resumescreening.controller;

import com.resumescreening.config.JpaAuditingConfig;
import com.resumescreening.config.SecurityConfig;
import com.resumescreening.dto.request.RegisterRequest;
import com.resumescreening.dto.response.UserResponse;
import com.resumescreening.exception.DuplicateResourceException;
import com.resumescreening.security.CustomUserDetailsService;
import com.resumescreening.service.UserService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AuthController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JpaAuditingConfig.class))
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("AuthController Web Layer Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    // SecurityConfig requires these beans
    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("GET /auth/login should return login page")
    void loginPageShouldLoad() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    @DisplayName("GET /auth/login with error param should show error message")
    void loginPageWithErrorShouldShowMessage() throws Exception {
        mockMvc.perform(get("/auth/login").param("error", "true"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @DisplayName("GET /auth/login with logout param should show message")
    void loginPageWithLogoutShouldShowMessage() throws Exception {
        mockMvc.perform(get("/auth/login").param("logout", "true"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("message"));
    }

    @Test
    @DisplayName("GET /auth/register should return register page")
    void registerPageShouldLoad() throws Exception {
        mockMvc.perform(get("/auth/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("registerRequest"));
    }

    @Test
    @DisplayName("POST /auth/register with valid data should redirect to login")
    void registrationWithValidDataShouldRedirect() throws Exception {
        UserResponse mockResponse = new UserResponse();
        mockResponse.setId(1L);
        mockResponse.setUsername("newuser");
        mockResponse.setRole("CANDIDATE");
        mockResponse.setCreatedAt(LocalDateTime.now());
        when(userService.register(any(RegisterRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/auth/register")
                .with(csrf())
                .param("username", "newuser")
                .param("email", "new@example.com")
                .param("password", "Password@1234")
                .param("confirmPassword", "Password@1234")
                .param("firstName", "New")
                .param("lastName", "User")
                .param("role", "CANDIDATE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));
    }

    @Test
    @DisplayName("POST /auth/register with mismatched passwords should return form")
    void registrationWithMismatchedPasswordsShouldReturnForm() throws Exception {
        mockMvc.perform(post("/auth/register")
                .with(csrf())
                .param("username", "newuser")
                .param("email", "new@example.com")
                .param("password", "Password@1234")
                .param("confirmPassword", "DifferentPass@1")
                .param("firstName", "New")
                .param("role", "CANDIDATE"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @DisplayName("POST /auth/register with duplicate username should return form with error")
    void registrationWithDuplicateUsernameShouldReturnError() throws Exception {
        when(userService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateResourceException("Username 'newuser' is already taken"));

        mockMvc.perform(post("/auth/register")
                .with(csrf())
                .param("username", "newuser")
                .param("email", "new@example.com")
                .param("password", "Password@1234")
                .param("confirmPassword", "Password@1234")
                .param("firstName", "New")
                .param("role", "CANDIDATE"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @DisplayName("POST /auth/register with invalid email should fail validation")
    void registrationWithInvalidEmailShouldFailValidation() throws Exception {
        mockMvc.perform(post("/auth/register")
                .with(csrf())
                .param("username", "newuser")
                .param("email", "not-a-valid-email")
                .param("password", "Password@1234")
                .param("confirmPassword", "Password@1234")
                .param("firstName", "New")
                .param("role", "CANDIDATE"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));
    }

    @Test
    @DisplayName("POST /auth/register with short username should fail validation")
    void registrationWithShortUsernameShouldFailValidation() throws Exception {
        mockMvc.perform(post("/auth/register")
                .with(csrf())
                .param("username", "ab")  // min 3 chars
                .param("email", "test@example.com")
                .param("password", "Password@1234")
                .param("confirmPassword", "Password@1234")
                .param("firstName", "Test")
                .param("role", "CANDIDATE"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));
    }

    @Test
    @DisplayName("POST /auth/register without CSRF should be rejected")
    void registrationWithoutCsrfShouldBeRejected() throws Exception {
        mockMvc.perform(post("/auth/register")
                .param("username", "newuser")
                .param("email", "new@example.com")
                .param("password", "Password@1234")
                .param("confirmPassword", "Password@1234")
                .param("firstName", "New")
                .param("role", "CANDIDATE"))
                .andExpect(status().isForbidden());
    }
}
