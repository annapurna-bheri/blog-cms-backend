package com.blogcms.service;
import com.blogcms.dto.request.CommentRequest;
import com.blogcms.dto.response.CommentResponse;
import com.blogcms.dto.response.PagedResponse;
import com.blogcms.entity.Comment;
import com.blogcms.entity.Post;
import com.blogcms.entity.User;
import com.blogcms.exception.ResourceNotFoundException;
import com.blogcms.exception.UnauthorizedException;
import com.blogcms.repository.CommentRepository;
import com.blogcms.repository.PostRepository;
import com.blogcms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository    postRepository;
    private final UserRepository    userRepository;

    // ── Add Comment ───────────────────────────────────────────────────
    @Transactional
    public CommentResponse addComment(Long postId, CommentRequest request) {
        User user = getCurrentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Post", "id", postId));

        Comment comment = Comment.builder()
                .content(request.getContent())
                .user(user)
                .post(post)
                .build();

        return toResponse(commentRepository.save(comment));
    }

    // ── Get Comments for a Post ───────────────────────────────────────
    @Transactional(readOnly = true)
    public PagedResponse<CommentResponse> getCommentsByPost(
            Long postId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> comments =
                commentRepository.findByPostIdOrderByCreatedAtDesc(
                        postId, pageable);

        return PagedResponse.<CommentResponse>builder()
                .content(comments.getContent()
                        .stream().map(this::toResponse).toList())
                .page(comments.getNumber())
                .size(comments.getSize())
                .totalElements(comments.getTotalElements())
                .totalPages(comments.getTotalPages())
                .last(comments.isLast())
                .build();
    }

    // ── Update Comment ────────────────────────────────────────────────
    @Transactional
    public CommentResponse updateComment(Long commentId, CommentRequest request) {
        Comment comment = findCommentOrThrow(commentId);
        User currentUser = getCurrentUser();

        if (!comment.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException(
                    "You can only edit your own comments");
        }

        comment.setContent(request.getContent());
        return toResponse(commentRepository.save(comment));
    }

    // ── Delete Comment ────────────────────────────────────────────────
    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = findCommentOrThrow(commentId);
        User currentUser = getCurrentUser();

        boolean isOwner = comment.getUser().getId()
                .equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole().name().equals("ROLE_ADMIN");

        if (!isOwner && !isAdmin) {
            throw new UnauthorizedException(
                    "You are not allowed to delete this comment");
        }

        commentRepository.delete(comment);
    }

    // ── Helpers ───────────────────────────────────────────────────────
    private Comment findCommentOrThrow(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Comment", "id", id));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", "email", email));
    }

    private CommentResponse toResponse(Comment comment) {
        CommentResponse.CommentUserInfo userInfo =
                CommentResponse.CommentUserInfo.builder()
                        .id(comment.getUser().getId())
                        .username(comment.getUser().getUsername())
                        .profilePicture(comment.getUser().getProfilePicture())
                        .build();

        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .user(userInfo)
                .postId(comment.getPost().getId())
                .build();
    }
}