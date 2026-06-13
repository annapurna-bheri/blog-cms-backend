package com.blogcms.repository;

import com.blogcms.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // Returns all comments for a specific post — paginated and newest first
    Page<Comment> findByPostIdOrderByCreatedAtDesc(Long postId, Pageable pageable);

    // Returns all comments written by a specific user — for profile page
    Page<Comment> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Count how many comments a post has (shown on post cards)
    Long countByPostId(Long postId);

    // Delete all comments on a post (called when a post is deleted)
    @Modifying
    @Query("DELETE FROM Comment c WHERE c.post.id = :postId")
    void deleteAllByPostId(@Param("postId") Long postId);
}