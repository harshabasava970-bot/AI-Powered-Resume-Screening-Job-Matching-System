package com.resumescreening.service;

import com.resumescreening.dto.request.RegisterRequest;
import com.resumescreening.dto.response.UserResponse;
import com.resumescreening.entity.*;
import com.resumescreening.exception.DuplicateResourceException;
import com.resumescreening.exception.UnauthorizedException;
import com.resumescreening.repository.*;
import com.resumescreening.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CandidateProfileRepository candidateProfileRepository;
    @Mock private RecruiterProfileRepository recruiterProfileRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UserServiceImpl userService;

    private RegisterRequest validCandidateRequest;
    private RegisterRequest validRecruiterRequest;

    @BeforeEach
    void setUp() {
        validCandidateRequest = new RegisterRequest();
        validCandidateRequest.setUsername("testuser");
        validCandidateRequest.setEmail("test@example.com");
        validCandidateRequest.setPassword("Test@1234");
        validCandidateRequest.setConfirmPassword("Test@1234");
        validCandidateRequest.setFirstName("Test");
        validCandidateRequest.setLastName("User");
        validCandidateRequest.setRole("CANDIDATE");

        validRecruiterRequest = new RegisterRequest();
        validRecruiterRequest.setUsername("recruiteruser");
        validRecruiterRequest.setEmail("recruiter@company.com");
        validRecruiterRequest.setPassword("Recruiter@1234");
        validRecruiterRequest.setConfirmPassword("Recruiter@1234");
        validRecruiterRequest.setFirstName("Recruiter");
        validRecruiterRequest.setLastName("User");
        validRecruiterRequest.setRole("RECRUITER");
    }

    @Test
    @DisplayName("Should register a candidate successfully")
    void shouldRegisterCandidate() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Test@1234")).thenReturn("$2a$12$hashedpassword");

        User savedUser = createUser(1L, "testuser", "test@example.com", Role.CANDIDATE);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse response = userService.register(validCandidateRequest);

        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getRole()).isEqualTo("CANDIDATE");

        verify(candidateProfileRepository, times(1)).save(any(CandidateProfile.class));
        verify(recruiterProfileRepository, never()).save(any());
        verify(passwordEncoder).encode("Test@1234");
    }

    @Test
    @DisplayName("Should register a recruiter successfully")
    void shouldRegisterRecruiter() {
        when(userRepository.existsByUsername("recruiteruser")).thenReturn(false);
        when(userRepository.existsByEmail("recruiter@company.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashed");

        User savedUser = createUser(2L, "recruiteruser", "recruiter@company.com", Role.RECRUITER);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse response = userService.register(validRecruiterRequest);

        assertThat(response.getRole()).isEqualTo("RECRUITER");
        verify(recruiterProfileRepository, times(1)).save(any(RecruiterProfile.class));
        verify(candidateProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException for duplicate username")
    void shouldThrowOnDuplicateUsername() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(validCandidateRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("testuser");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException for duplicate email")
    void shouldThrowOnDuplicateEmail() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(validCandidateRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("test@example.com");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when passwords do not match")
    void shouldThrowWhenPasswordsDoNotMatch() {
        validCandidateRequest.setConfirmPassword("DifferentPassword@1");

        assertThatThrownBy(() -> userService.register(validCandidateRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Passwords do not match");
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when registering as ADMIN")
    void shouldThrowWhenRegisteringAsAdmin() {
        validCandidateRequest.setRole("ADMIN");
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        assertThatThrownBy(() -> userService.register(validCandidateRequest))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for invalid role")
    void shouldThrowForInvalidRole() {
        validCandidateRequest.setRole("SUPERUSER");
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        assertThatThrownBy(() -> userService.register(validCandidateRequest))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Password should be encoded with BCrypt")
    void passwordShouldBeEncoded() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("Test@1234")).thenReturn("$2a$12$encoded");

        User savedUser = createUser(1L, "testuser", "test@example.com", Role.CANDIDATE);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        userService.register(validCandidateRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("$2a$12$encoded");
        assertThat(userCaptor.getValue().getPassword()).isNotEqualTo("Test@1234");
    }

    @Test
    @DisplayName("existsByUsername should delegate to repository")
    void existsByUsernameShouldDelegate() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);
        assertThat(userService.existsByUsername("testuser")).isTrue();

        when(userRepository.existsByUsername("unknown")).thenReturn(false);
        assertThat(userService.existsByUsername("unknown")).isFalse();
    }

    @Test
    @DisplayName("existsByEmail should delegate to repository")
    void existsByEmailShouldDelegate() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);
        assertThat(userService.existsByEmail("test@example.com")).isTrue();
    }

    private User createUser(Long id, String username, String email, Role role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(role);
        user.setEnabled(true);
        // Simulate @CreatedDate via reflection or set manually:
        try {
            var field = User.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(user, LocalDateTime.now());
            var field2 = User.class.getDeclaredField("updatedAt");
            field2.setAccessible(true);
            field2.set(user, LocalDateTime.now());
        } catch (Exception ignored) {}
        return user;
    }
}
