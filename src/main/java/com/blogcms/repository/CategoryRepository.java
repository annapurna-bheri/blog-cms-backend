package com.blogcms.repository;

import com.blogcms.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Used to find a category by its URL slug e.g. "spring-boot"
    Optional<Category> findBySlug(String slug);

    // Used during category creation to prevent duplicate names
    Boolean existsByName(String name);

    // Used during category creation to prevent duplicate slugs
    Boolean existsBySlug(String slug);
}
