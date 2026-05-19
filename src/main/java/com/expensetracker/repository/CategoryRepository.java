package com.expensetracker.repository;

import com.expensetracker.model.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("""
            SELECT c FROM Category c
            WHERE c.defaultCategory = true OR c.user.id = :userId
            ORDER BY c.defaultCategory DESC, c.name ASC
            """)
    List<Category> findDefaultsAndByUserId(@Param("userId") Long userId);

    Optional<Category> findByIdAndUserId(Long id, Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);
}
