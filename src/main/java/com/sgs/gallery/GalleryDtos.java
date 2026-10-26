package com.sgs.gallery;

import com.sgs.category.CategoryDtos.CategoryRef;
import com.sgs.common.SafeUrl;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

public final class GalleryDtos {
    private GalleryDtos() {}

    /** uploadedBy is only filled for authenticated callers; null fields are omitted from the JSON. */
    public record GalleryDto(Long id, String title, String description, String socialLink, CategoryRef category,
                             String imageUrl, String thumbUrl, Instant createdAt, String uploadedBy) {}

    /** multipart/form-data fields for creating a gallery image. */
    @Getter
    @Setter
    public static class GalleryForm {
        @NotBlank @Size(max = 200)
        private String title;

        @Size(max = 10000, message = "Description can be at most 10,000 characters")
        private String description;

        @SafeUrl
        private String socialLink;

        @NotNull
        private Long categoryId;

        private MultipartFile image;
    }

    public record GalleryUpdateRequest(@NotBlank @Size(max = 200) String title,
                                       @Size(max = 10000, message = "Description can be at most 10,000 characters") String description,
                                       @SafeUrl String socialLink,
                                       @NotNull Long categoryId) {}
}
