package com.blogcms.service;
import com.blogcms.dto.request.LoginRequest;
import com.blogcms.dto.request.RegisterRequest;
import com.blogcms.dto.response.AuthResponse;
import com.blogcms.entity.Role;
import com.blogcms.entity.User;
import com.blogcms.exception.BadRequestException;
import com.blogcms.repository.UserRepository;
import com.blogcms.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository      userRepository;
    private final PasswordEncoder     passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil             jwtUtil;

    // ── Register ──────────────────────────────────────────────────────
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        // Check for duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException(
                    "Email is already registered: " + request.getEmail());
        }

        // Check for duplicate username
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException(
                    "Username is already taken: " + request.getUsername());
        }

        // Build and save the new user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(Role.ROLE_USER)          // all new users get USER role
                .active(true)
                .build();

        userRepository.save(user);

        // Generate token and return auth response immediately (auto-login)
        String token = jwtUtil.generateToken(user.getEmail());
        return buildAuthResponse(token, user);
    }

    // ── Login ─────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {

        // Resolve identifier — could be email or username
        User user = userRepository
                .findByEmailOrUsername(request.getIdentifier())
                .orElseThrow(() ->
                        new BadRequestException("Invalid email/username or password"));

        // Check if account is active
        if (!user.getActive()) {
            throw new BadRequestException("Your account has been deactivated");
        }

        // Delegate credential validation to Spring Security
        // Throws BadCredentialsException automatically if password is wrong
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        user.getEmail(),          // must match what loadUserByUsername uses
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(auth);

        String token = jwtUtil.generateToken(user.getEmail());
        return buildAuthResponse(token, user);
    }

    // ── Helper ────────────────────────────────────────────────────────
    private AuthResponse buildAuthResponse(String token, User user) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}