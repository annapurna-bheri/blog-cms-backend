package com.blogcms.service;
import com.blogcms.dto.request.UpdateProfileRequest;
import com.blogcms.dto.response.UserResponse;
import com.blogcms.entity.User;
import com.blogcms.exception.ResourceNotFoundException;
import com.blogcms.exception.UnauthorizedException;
import com.blogcms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // ── Get Current User Profile ──────────────────────────────────────
    @Transactional(readOnly = true)
    public UserResponse getMyProfile() {
        return toResponse(getCurrentUser());
    }

    // ── Update Current User Profile ───────────────────────────────────
    @Transactional
    public UserResponse updateMyProfile(UpdateProfileRequest request) {
        User user = getCurrentUser();

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getProfilePicture() != null) {
            user.setProfilePicture(request.getProfilePicture());
        }

        return toResponse(userRepository.save(user));
    }

    // ── Get User by ID (public profile) ──────────────────────────────
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return toResponse(findUserOrThrow(id));
    }

    // ── Admin: Get All Users ──────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Admin: Deactivate / Reactivate User ───────────────────────────
    @Transactional
    public UserResponse toggleUserActive(Long id) {
        User user = findUserOrThrow(id);
        User currentUser = getCurrentUser();

        // Prevent admin from deactivating themselves
        if (user.getId().equals(currentUser.getId())) {
            throw new UnauthorizedException(
                    "You cannot deactivate your own account");
        }

        user.setActive(!user.getActive());
        return toResponse(userRepository.save(user));
    }

    // ── Helpers ───────────────────────────────────────────────────────
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", "email", email));
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", "id", id));
    }

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .bio(user.getBio())
                .profilePicture(user.getProfilePicture())
                .role(user.getRole().name())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
