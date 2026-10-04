package com.resumescreening.service;

import com.resumescreening.dto.request.ProfileUpdateRequest;
import com.resumescreening.dto.request.RegisterRequest;
import com.resumescreening.dto.response.UserResponse;
import com.resumescreening.entity.User;

import java.util.List;

public interface UserService {

    UserResponse register(RegisterRequest request);

    User findByUsername(String username);

    User findById(Long id);

    UserResponse updateCandidateProfile(String username, ProfileUpdateRequest request);

    UserResponse updateRecruiterProfile(String username, ProfileUpdateRequest request);

    List<UserResponse> getAllUsers();

    List<UserResponse> getUsersByRole(String role);

    void toggleUserEnabled(Long userId);

    void deleteUser(Long userId);

    UserResponse getCurrentUserResponse(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
