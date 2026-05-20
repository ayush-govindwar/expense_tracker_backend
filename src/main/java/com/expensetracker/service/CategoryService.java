package com.expensetracker.service;

import com.expensetracker.exception.ResourceNotFoundException;
import com.expensetracker.model.dto.request.CategoryRequest;
import com.expensetracker.model.dto.response.CategoryResponse;
import com.expensetracker.model.entity.Category;
import com.expensetracker.model.entity.User;
import com.expensetracker.repository.CategoryRepository;
import com.expensetracker.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryResponse> listForUser(Long userId) {
        return categoryRepository.findDefaultsAndByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CategoryResponse create(UserPrincipal principal, CategoryRequest request) {
        Category category = new Category();
        category.setName(request.getName());
        category.setIcon(request.getIcon());
        category.setUser(principal.getUser());
        category.setDefault(false);
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(UserPrincipal principal, Long id, CategoryRequest request) {
        Category category = getOwnedCategory(principal.getId(), id);
        category.setName(request.getName());
        category.setIcon(request.getIcon());
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(UserPrincipal principal, Long id) {
        Category category = getOwnedCategory(principal.getId(), id);
        categoryRepository.delete(category);
    }

    private Category getOwnedCategory(Long userId, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (category.isDefault()) {
            throw new IllegalArgumentException("Cannot modify default category");
        }

        if (category.getUser() == null || !category.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Category not found");
        }

        return category;
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getIcon(),
                category.isDefault()
        );
    }
}
