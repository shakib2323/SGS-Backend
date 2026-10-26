package com.sgs.tool;

import com.sgs.common.SafeUrl;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

public final class ToolDtos {
    private ToolDtos() {}

    public record ToolDto(Long id, String title, String description, String toolLink, String imageUrl,
                          String thumbUrl, int displayOrder, boolean active) {}

    @Getter
    @Setter
    public static class ToolForm {
        @NotBlank @Size(max = 150)
        private String title;
        @Size(max = 10000)
        private String description;
        @SafeUrl
        private String toolLink;
        private Integer displayOrder;
        private Boolean active;
        private MultipartFile image;
    }

    public record ToolUpdateRequest(@NotBlank @Size(max = 150) String title,
                                    @Size(max = 10000) String description,
                                    @SafeUrl String toolLink,
                                    Integer displayOrder,
                                    Boolean active) {}
}
