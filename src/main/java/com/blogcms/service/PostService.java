package com.blogcms.service;
import com.blogcms.dto.request.PostRequest;
import com.blogcms.dto.response.PagedResponse;
import com.blogcms.dto.response.PostResponse;
import com.blogcms.entity.Category;
import com.blogcms.entity.Post;
import com.blogcms.entity.User;
import com.blogcms.exception.BadRequestException;
import com.blogcms.exception.ResourceNotFoundException;
import com.blogcms.exception.UnauthorizedException;
import com.blogcms.repository.CategoryRepository;
import com.blogcms.repository.CommentRepository;
import com.blogcms.repository.PostRepository;
import com.blogcms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository     postRepository;
    private final UserRepository     userRepository;
    private final CategoryRepository categoryRepository;
    private final CommentRepository  commentRepository;

    // ── Create ────────────────────────────────────────────────────────
    @Transactional
    public PostResponse createPost(PostRequest request) {
        User author = getCurrentUser();

        String slug = generateUniqueSlug(request.getTitle());

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Category", "id", request.getCategoryId()));
        }

        Post post = Post.builder()
                .title(request.getTitle())
                .slug(slug)
                .summary(request.getSummary())
                .content(request.getContent())
                .coverImage(request.getCoverImage())
                .published(request.getPublished())
                .publishedAt(Boolean.TRUE.equals(request.getPublished())
                        ? LocalDateTime.now() : null)
                .author(author)
                .category(category)
                .build();

        return toResponse(postRepository.save(post));
    }

    // ── Get All Published Posts (public) ──────────────────────────────
    @Transactional(readOnly = true)
    public PagedResponse<PostResponse> getAllPublishedPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());
        Page<Post> posts =
                postRepository.findByPublishedTrueOrderByCreatedAtDesc(pageable);
        return toPagedResponse(posts);
    }

    // ── Get Single Post by Slug ───────────────────────────────────────
    @Transactional
    public PostResponse getPostBySlug(String slug) {
        Post post = postRepository.findBySlug(slug)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Post", "slug", slug));

        // Increment view count efficiently (no full entity reload)
        postRepository.incrementViewCount(post.getId());
        post.setViewCount(post.getViewCount() + 1);

        return toResponse(post);
    }

    // ── Get Post by ID ────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public PostResponse getPostById(Long id) {
        return toResponse(findPostOrThrow(id));
    }

    // ── Get Posts by Category ─────────────────────────────────────────
    @Transactional(readOnly = true)
    public PagedResponse<PostResponse> getPostsByCategory(
            Long categoryId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());
        Page<Post> posts =
                postRepository.findByCategoryIdAndPublishedTrue(
                        categoryId, pageable);
        return toPagedResponse(posts);
    }

    // ── Get My Posts (dashboard) ──────────────────────────────────────
    @Transactional(readOnly = true)
    public PagedResponse<PostResponse> getMyPosts(int page, int size) {
        User author = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());
        Page<Post> posts =
                postRepository.findByAuthorOrderByCreatedAtDesc(
                        author, pageable);
        return toPagedResponse(posts);
    }

    // ── Search Posts ──────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public PagedResponse<PostResponse> searchPosts(
            String keyword, int page, int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());
        Page<Post> posts =
                postRepository.searchPublishedPosts(keyword, pageable);
        return toPagedResponse(posts);
    }

    // ── Update Post ───────────────────────────────────────────────────
    @Transactional
    public PostResponse updatePost(Long id, PostRequest request) {
        Post post = findPostOrThrow(id);
        User currentUser = getCurrentUser();

        // Only the author or an admin can edit
        if (!post.getAuthor().getId().equals(currentUser.getId())
                && !isAdmin(currentUser)) {
            throw new UnauthorizedException(
                    "You are not allowed to edit this post");
        }

        // Regenerate slug only if title changed
        if (!post.getTitle().equals(request.getTitle())) {
            post.setSlug(generateUniqueSlug(request.getTitle()));
        }

        if (request.getCategoryId() != null) {
            Category category = categoryRepository
                    .findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Category", "id", request.getCategoryId()));
            post.setCategory(category);
        } else {
            post.setCategory(null);
        }

        // Handle publish/unpublish transitions
        boolean wasPublished = Boolean.TRUE.equals(post.getPublished());
        boolean willPublish  = Boolean.TRUE.equals(request.getPublished());

        if (!wasPublished && willPublish) {
            post.setPublishedAt(LocalDateTime.now());
        } else if (wasPublished && !willPublish) {
            post.setPublishedAt(null);
        }

        post.setTitle(request.getTitle());
        post.setSummary(request.getSummary());
        post.setContent(request.getContent());
        post.setCoverImage(request.getCoverImage());
        post.setPublished(request.getPublished());

        return toResponse(postRepository.save(post));
    }

    // ── Toggle Publish / Unpublish ────────────────────────────────────
    @Transactional
    public PostResponse togglePublish(Long id) {
        Post post = findPostOrThrow(id);
        User currentUser = getCurrentUser();

        if (!post.getAuthor().getId().equals(currentUser.getId())
                && !isAdmin(currentUser)) {
            throw new UnauthorizedException(
                    "You are not allowed to publish/unpublish this post");
        }

        boolean newState = !Boolean.TRUE.equals(post.getPublished());
        post.setPublished(newState);
        post.setPublishedAt(newState ? LocalDateTime.now() : null);

        return toResponse(postRepository.save(post));
    }

    // ── Delete Post ───────────────────────────────────────────────────
    @Transactional
    public void deletePost(Long id) {
        Post post = findPostOrThrow(id);
        User currentUser = getCurrentUser();

        if (!post.getAuthor().getId().equals(currentUser.getId())
                && !isAdmin(currentUser)) {
            throw new UnauthorizedException(
                    "You are not allowed to delete this post");
        }

        postRepository.delete(post);
    }

    // ── Helpers ───────────────────────────────────────────────────────

    // Gets the currently authenticated user from the security context
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", "email", email));
    }

    private Post findPostOrThrow(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Post", "id", id));
    }

    private boolean isAdmin(User user) {
        return user.getRole().name().equals("ROLE_ADMIN");
    }

    // Converts "My First Post" → "my-first-post"
    // Appends a counter if the slug already exists e.g. "my-first-post-2"
    private String generateUniqueSlug(String title) {
        String base = title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();

        String slug = base;
        int counter = 2;
        while (postRepository.existsBySlug(slug)) {
            slug = base + "-" + counter++;
        }
        return slug;
    }

    // Converts a Post entity to a PostResponse DTO
    private PostResponse toResponse(Post post) {
        Long commentCount = commentRepository.countByPostId(post.getId());

        PostResponse.AuthorInfo authorInfo = PostResponse.AuthorInfo.builder()
                .id(post.getAuthor().getId())
                .username(post.getAuthor().getUsername())
                .fullName(post.getAuthor().getFullName())
                .profilePicture(post.getAuthor().getProfilePicture())
                .build();

        PostResponse.CategoryInfo categoryInfo = null;
        if (post.getCategory() != null) {
            categoryInfo = PostResponse.CategoryInfo.builder()
                    .id(post.getCategory().getId())
                    .name(post.getCategory().getName())
                    .slug(post.getCategory().getSlug())
                    .build();
        }

        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .slug(post.getSlug())
                .summary(post.getSummary())
                .content(post.getContent())
                .coverImage(post.getCoverImage())
                .published(post.getPublished())
                .viewCount(post.getViewCount())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .publishedAt(post.getPublishedAt())
                .author(authorInfo)
                .category(categoryInfo)
                .commentCount(commentCount)
                .build();
    }

    private PagedResponse<PostResponse> toPagedResponse(Page<Post> page) {
        return PagedResponse.<PostResponse>builder()
                .content(page.getContent().stream().map(this::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}