package com.sgs.offering;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class ServiceDtos {
    private ServiceDtos() {}

    public record ServiceImageDto(Long id, String imageUrl, String thumbUrl, int displayOrder) {}

    /** images are ordered for the carousel; the frontend auto-plays and supports swipe (e.g. Swiper). */
    public record ServiceDto(Long id, String title, String slug, String shortDescription, String description,
                             String iconClass, String warranty, String priceNote, boolean featured, boolean active,
                             int displayOrder, String metaTitle, String metaDescription, List<String> features,
                             List<ServiceImageDto> images) {}

    public record ServiceRequest(
            @NotBlank @Size(max = 150) String title,
            @Size(max = 300) String shortDescription,
            @Size(max = 10000) String description,
            @Size(max = 60) @Pattern(regexp = "^[a-zA-Z0-9 _-]*$", message = "Only letters, digits, space, - and _") String iconClass,
            @Size(max = 150) String warranty,
            @Size(max = 120) String priceNote,
            Boolean featured,
            Boolean active,
            Integer displayOrder,
            @Size(max = 160) String metaTitle,
            @Size(max = 300) String metaDescription,
            @Size(max = 20, message = "At most 20 features") List<@NotBlank @Size(max = 200) String> features) {}

    public record ReorderImagesRequest(@NotEmpty List<Long> imageIds) {}
}
