package com.sgs.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CategoryDtos {
    private CategoryDtos() {}

    public record CategoryDto(Long id, String name, String slug, int displayOrder, boolean active) {
        public static CategoryDto from(Category c) {
            return new CategoryDto(c.getId(), c.getName(), c.getSlug(), c.getDisplayOrder(), c.isActive());
        }
    }

    /** Small reference embedded in gallery/project responses. */
    public record CategoryRef(Long id, String name, String slug) {
        public static CategoryRef from(Category c) {
            return new CategoryRef(c.getId(), c.getName(), c.getSlug());
        }
    }

    public record CategoryRequest(@NotBlank @Size(max = 60) String name, Integer displayOrder, Boolean active) {}
}
