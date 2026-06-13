package com.blogcms.service;
import com.blogcms.dto.request.CategoryRequest;
import com.blogcms.dto.response.CategoryResponse;
import com.blogcms.entity.Category;
import com.blogcms.exception.BadRequestException;
import com.blogcms.exception.ResourceNotFoundException;
import com.blogcms.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // ── Create ────────────────────────────────────────────────────────
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {

        if (categoryRepository.existsByName(request.getName())) {
            throw new BadRequestException(
                    "Category already exists: " + request.getName());
        }

        Category category = Category.builder()
                .name(request.getName())
                .slug(generateSlug(request.getName()))
                .description(request.getDescription())
                .build();

        return toResponse(categoryRepository.save(category));
    }

    // ── Read All ──────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Read One ──────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        return toResponse(findCategoryOrThrow(id));
    }

    // ── Update ────────────────────────────────────────────────────────
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = findCategoryOrThrow(id);

        // Only check name uniqueness if the name actually changed
        if (!category.getName().equalsIgnoreCase(request.getName())
                && categoryRepository.existsByName(request.getName())) {
            throw new BadRequestException(
                    "Category name already exists: " + request.getName());
        }

        category.setName(request.getName());
        category.setSlug(generateSlug(request.getName()));
        category.setDescription(request.getDescription());

        return toResponse(categoryRepository.save(category));
    }

    // ── Delete ────────────────────────────────────────────────────────
    @Transactional
    public void deleteCategory(Long id) {
        Category category = findCategoryOrThrow(id);
        categoryRepository.delete(category);
    }

    // ── Helpers ───────────────────────────────────────────────────────
    private Category findCategoryOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category", "id", id));
    }

    // Converts "Spring Boot Tips" → "spring-boot-tips"
    public String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();
    }

    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .postCount(category.getPosts().size())
                .build();
    }
}
