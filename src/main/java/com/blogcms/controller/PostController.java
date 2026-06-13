package com.blogcms.controller;
import com.blogcms.dto.request.PostRequest;
import com.blogcms.dto.response.ApiResponse;
import com.blogcms.dto.response.PagedResponse;
import com.blogcms.dto.response.PostResponse;
import com.blogcms.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // ── Public Endpoints ──────────────────────────────────────────────

    // GET /api/posts?page=0&size=10
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<PostResponse>>>
            getAllPublishedPosts(
                    @RequestParam(defaultValue = "0")  int page,
                    @RequestParam(defaultValue = "10") int size) {

        PagedResponse<PostResponse> data =
                postService.getAllPublishedPosts(page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Posts fetched successfully", data));
    }

    // GET /api/posts/slug/{slug}
    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<PostResponse>> getPostBySlug(
            @PathVariable String slug) {

        PostResponse data = postService.getPostBySlug(slug);
        return ResponseEntity.ok(
                ApiResponse.success("Post fetched successfully", data));
    }

    // GET /api/posts/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostResponse>> getPostById(
            @PathVariable Long id) {

        PostResponse data = postService.getPostById(id);
        return ResponseEntity.ok(
                ApiResponse.success("Post fetched successfully", data));
    }

    // GET /api/posts/category/{categoryId}?page=0&size=10
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<PagedResponse<PostResponse>>>
            getPostsByCategory(
                    @PathVariable Long categoryId,
                    @RequestParam(defaultValue = "0")  int page,
                    @RequestParam(defaultValue = "10") int size) {

        PagedResponse<PostResponse> data =
                postService.getPostsByCategory(categoryId, page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Posts fetched successfully", data));
    }

    // GET /api/posts/search?keyword=spring&page=0&size=10
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PagedResponse<PostResponse>>> searchPosts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        PagedResponse<PostResponse> data =
                postService.searchPosts(keyword, page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Search results fetched", data));
    }

    // ── Authenticated Endpoints ───────────────────────────────────────

    // GET /api/posts/my-posts?page=0&size=10
    @GetMapping("/my-posts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PagedResponse<PostResponse>>> getMyPosts(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        PagedResponse<PostResponse> data = postService.getMyPosts(page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Your posts fetched successfully", data));
    }

    // POST /api/posts
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @Valid @RequestBody PostRequest request) {

        PostResponse data = postService.createPost(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Post created successfully", data));
    }

    // PUT /api/posts/{id}
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody PostRequest request) {

        PostResponse data = postService.updatePost(id, request);
        return ResponseEntity.ok(
                ApiResponse.success("Post updated successfully", data));
    }

    // PATCH /api/posts/{id}/toggle-publish
    @PatchMapping("/{id}/toggle-publish")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PostResponse>> togglePublish(
            @PathVariable Long id) {

        PostResponse data = postService.togglePublish(id);
        return ResponseEntity.ok(
                ApiResponse.success("Post publish status updated", data));
    }

    // DELETE /api/posts/{id}
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable Long id) {

        postService.deletePost(id);
        return ResponseEntity.ok(
                ApiResponse.success("Post deleted successfully"));
    }
}