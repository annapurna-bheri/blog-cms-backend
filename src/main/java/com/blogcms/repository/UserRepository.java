package com.blogcms.repository;
import com.blogcms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Used during login to find user by email
    Optional<User> findByEmail(String email);

    // Used during registration to check if username is taken
    Optional<User> findByUsername(String username);

    // Used during registration validation
    Boolean existsByEmail(String email);

    // Used during registration validation
    Boolean existsByUsername(String username);

    // Finds a user by email or username (useful for flexible login)
    @Query("SELECT u FROM User u WHERE u.email = :identifier OR u.username = :identifier")
    Optional<User> findByEmailOrUsername(@Param("identifier") String identifier);
}