package com.blogcms.controller;
import com.blogcms.dto.request.CommentRequest;
import com.blogcms.dto.response.ApiResponse;
import com.blogcms.dto.response.CommentResponse;
import com.blogcms.dto.response.PagedResponse;
import com.blogcms.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // GET /api/posts/{postId}/comments?page=0&size=10 — public
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<PagedResponse<CommentResponse>>>
            getCommentsByPost(
                    @PathVariable Long postId,
                    @RequestParam(defaultValue = "0")  int page,
                    @RequestParam(defaultValue = "10") int size) {

        PagedResponse<CommentResponse> data =
                commentService.getCommentsByPost(postId, page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Comments fetched successfully", data));
    }

    // POST /api/posts/{postId}/comments — authenticated
    @PostMapping("/posts/{postId}/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommentRequest request) {

        CommentResponse data = commentService.addComment(postId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Comment added successfully", data));
    }

    // PUT /api/comments/{commentId} — authenticated (owner only)
    @PutMapping("/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest request) {

        CommentResponse data =
                commentService.updateComment(commentId, request);
        return ResponseEntity.ok(
                ApiResponse.success("Comment updated successfully", data));
    }

    // DELETE /api/comments/{commentId} — authenticated (owner or admin)
    @DeleteMapping("/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId) {

        commentService.deleteComment(commentId);
        return ResponseEntity.ok(
                ApiResponse.success("Comment deleted successfully"));
    }
}