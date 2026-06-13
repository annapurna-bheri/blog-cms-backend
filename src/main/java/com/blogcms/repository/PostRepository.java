package com.blogcms.repository;

import com.blogcms.entity.Post;
import com.blogcms.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // Finds a post by its URL slug e.g. "my-first-blog-post"
    Optional<Post> findBySlug(String slug);

    // Checks if a slug already exists (for uniqueness before saving)
    Boolean existsBySlug(String slug);

    // Returns all PUBLISHED posts — paginated (for public blog listing)
    Page<Post> findByPublishedTrueOrderByCreatedAtDesc(Pageable pageable);

    // Returns all posts by a specific author — paginated (for dashboard)
    Page<Post> findByAuthorOrderByCreatedAtDesc(User author, Pageable pageable);

    // Returns all posts in a specific category — paginated
    Page<Post> findByCategoryIdAndPublishedTrue(Long categoryId, Pageable pageable);

    // Full-text search on title and content — paginated
    @Query("""
            SELECT p FROM Post p
            WHERE p.published = true
            AND (
                LOWER(p.title)   LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR
                LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR
                LOWER(p.summary) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            ORDER BY p.createdAt DESC
            """)
    Page<Post> searchPublishedPosts(@Param("keyword") String keyword, Pageable pageable);

    // Admin-only: search across ALL posts including drafts
    @Query("""
            SELECT p FROM Post p
            WHERE
                LOWER(p.title)   LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR
                LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY p.createdAt DESC
            """)
    Page<Post> searchAllPosts(@Param("keyword") String keyword, Pageable pageable);

    // Increments view count without loading the full entity (efficient)
    @Modifying
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    void incrementViewCount(@Param("id") Long id);

    // Dashboard stat: count total posts by a specific author
    Long countByAuthor(User author);

    // Dashboard stat: count published posts by a specific author
    Long countByAuthorAndPublishedTrue(User author);
}
