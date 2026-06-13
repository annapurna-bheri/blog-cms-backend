package com.blogcms.controller;
import com.blogcms.dto.request.UpdateProfileRequest;
import com.blogcms.dto.response.ApiResponse;
import com.blogcms.dto.response.UserResponse;
import com.blogcms.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // GET /api/users/me — authenticated
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile() {
        UserResponse data = userService.getMyProfile();
        return ResponseEntity.ok(
                ApiResponse.success("Profile fetched successfully", data));
    }

    // PUT /api/users/me — authenticated
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request) {

        UserResponse data = userService.updateMyProfile(request);
        return ResponseEntity.ok(
                ApiResponse.success("Profile updated successfully", data));
    }

    // GET /api/users/{id} — public (view author profile)
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @PathVariable Long id) {

        UserResponse data = userService.getUserById(id);
        return ResponseEntity.ok(
                ApiResponse.success("User fetched successfully", data));
    }

    // ── Admin Endpoints ───────────────────────────────────────────────

    // GET /api/users — ADMIN only
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        List<UserResponse> data = userService.getAllUsers();
        return ResponseEntity.ok(
                ApiResponse.success("Users fetched successfully", data));
    }

    // PATCH /api/users/{id}/toggle-active — ADMIN only
    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> toggleUserActive(
            @PathVariable Long id) {

        UserResponse data = userService.toggleUserActive(id);
        return ResponseEntity.ok(
                ApiResponse.success("User status updated successfully", data));
    }
}