package com.expensetracker.config;

import com.expensetracker.model.entity.Category;
import com.expensetracker.repository.CategoryRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DefaultCategorySeeder implements ApplicationRunner {

    private final CategoryRepository categoryRepository;

    public DefaultCategorySeeder(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (categoryRepository.countByDefaultCategoryTrue() > 0) {
            return;
        }

        seed("Food", "🍔");
        seed("Transport", "🚗");
        seed("Shopping", "🛍️");
        seed("Health", "🏥");
        seed("Entertainment", "🎬");
        seed("Bills", "⚡");
        seed("Education", "📚");
        seed("Others", "💼");
    }

    private void seed(String name, String icon) {
        Category category = new Category();
        category.setName(name);
        category.setIcon(icon);
        category.setDefault(true);
        category.setUser(null);
        categoryRepository.save(category);
    }
}
