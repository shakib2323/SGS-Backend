package com.sgs.project;

import com.sgs.category.CategoryDtos.CategoryRef;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

public final class ProjectDtos {
    private ProjectDtos() {}

    public record ProjectDto(Long id, String title, String description, CategoryRef category, String location,
                             String clientName, int startedYear, Integer completedYear, ProjectStatus status,
                             Integer areaSqFt, boolean featured, String imageUrl, String thumbUrl, Instant createdAt) {}

    @Getter
    @Setter
    public static class ProjectForm {
        @NotBlank @Size(max = 200)
        private String title;
        @Size(max = 10000)
        private String description;
        @NotNull
        private Long categoryId;
        @Size(max = 150)
        private String location;
        @Size(max = 150)
        private String clientName;
        @NotNull @Min(1990) @Max(2100)
        private Integer startedYear;
        @Min(1990) @Max(2100)
        private Integer completedYear;
        @NotNull
        private ProjectStatus status;
        @Min(1)
        private Integer areaSqFt;
        private Boolean featured;
        private MultipartFile image;
    }

    public record ProjectUpdateRequest(@NotBlank @Size(max = 200) String title,
                                       @Size(max = 10000) String description,
                                       @NotNull Long categoryId,
                                       @Size(max = 150) String location,
                                       @Size(max = 150) String clientName,
                                       @NotNull @Min(1990) @Max(2100) Integer startedYear,
                                       @Min(1990) @Max(2100) Integer completedYear,
                                       @NotNull ProjectStatus status,
                                       @Min(1) Integer areaSqFt,
                                       Boolean featured) {}
}
