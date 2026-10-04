package com.resumescreening.service.impl;

import com.resumescreening.dto.request.ProfileUpdateRequest;
import com.resumescreening.dto.request.RegisterRequest;
import com.resumescreening.dto.response.UserResponse;
import com.resumescreening.entity.*;
import com.resumescreening.exception.DuplicateResourceException;
import com.resumescreening.exception.ResourceNotFoundException;
import com.resumescreening.exception.UnauthorizedException;
import com.resumescreening.repository.*;
import com.resumescreening.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           CandidateProfileRepository candidateProfileRepository,
                           RecruiterProfileRepository recruiterProfileRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.candidateProfileRepository = candidateProfileRepository;
        this.recruiterProfileRepository = recruiterProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponse register(RegisterRequest request) {
        // Validate passwords match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        // Check duplicates
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already registered");
        }

        // Validate role
        Role role;
        try {
            role = Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role. Must be CANDIDATE or RECRUITER");
        }
        if (role == Role.ADMIN) {
            throw new UnauthorizedException("Cannot register as ADMIN");
        }

        // Create user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(role);
        user.setEnabled(true);
        user = userRepository.save(user);

        // Create role-specific profile
        if (role == Role.CANDIDATE) {
            CandidateProfile profile = new CandidateProfile();
            profile.setUser(user);
            candidateProfileRepository.save(profile);
            log.info("Candidate profile created for user: {}", user.getUsername());
        } else if (role == Role.RECRUITER) {
            RecruiterProfile profile = new RecruiterProfile();
            profile.setUser(user);
            recruiterProfileRepository.save(profile);
            log.info("Recruiter profile created for user: {}", user.getUsername());
        }

        return UserResponse.from(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", null));
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Override
    public UserResponse updateCandidateProfile(String username, ProfileUpdateRequest request) {
        User user = findByUsername(username);
        updateUserFields(user, request);
        userRepository.save(user);

        CandidateProfile profile = candidateProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found"));

        if (request.getPhone() != null)        profile.setPhone(request.getPhone());
        if (request.getLocation() != null)     profile.setLocation(request.getLocation());
        if (request.getHeadline() != null)     profile.setHeadline(request.getHeadline());
        if (request.getSummary() != null)      profile.setSummary(request.getSummary());
        if (request.getLinkedinUrl() != null)  profile.setLinkedinUrl(request.getLinkedinUrl());
        if (request.getGithubUrl() != null)    profile.setGithubUrl(request.getGithubUrl());
        if (request.getPortfolioUrl() != null) profile.setPortfolioUrl(request.getPortfolioUrl());
        candidateProfileRepository.save(profile);

        return UserResponse.from(user);
    }

    @Override
    public UserResponse updateRecruiterProfile(String username, ProfileUpdateRequest request) {
        User user = findByUsername(username);
        updateUserFields(user, request);
        userRepository.save(user);

        RecruiterProfile profile = recruiterProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile not found"));

        if (request.getPhone() != null)          profile.setPhone(request.getPhone());
        if (request.getCompanyName() != null)    profile.setCompanyName(request.getCompanyName());
        if (request.getJobTitle() != null)       profile.setJobTitle(request.getJobTitle());
        if (request.getCompanyWebsite() != null) profile.setCompanyWebsite(request.getCompanyWebsite());
        if (request.getIndustry() != null)       profile.setIndustry(request.getIndustry());
        recruiterProfileRepository.save(profile);

        return UserResponse.from(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAllOrderByCreatedAtDesc().stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByRole(String role) {
        Role roleEnum = Role.valueOf(role.toUpperCase());
        return userRepository.findByRole(roleEnum).stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public void toggleUserEnabled(Long userId) {
        User user = findById(userId);
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        log.info("User {} enabled status toggled to: {}", user.getUsername(), user.isEnabled());
    }

    @Override
    public void deleteUser(Long userId) {
        User user = findById(userId);
        userRepository.delete(user);
        log.info("User {} deleted", user.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserResponse(String username) {
        return UserResponse.from(findByUsername(username));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    private void updateUserFields(User user, ProfileUpdateRequest request) {
        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
    }
}
