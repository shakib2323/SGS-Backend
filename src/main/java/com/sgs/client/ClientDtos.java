package com.sgs.client;

import com.sgs.common.SafeUrl;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

public final class ClientDtos {
    private ClientDtos() {}

    public record ClientDto(Long id, String name, String logoUrl, String websiteUrl, int displayOrder, boolean active) {}

    @Getter
    @Setter
    public static class ClientForm {
        @NotBlank @Size(max = 150)
        private String name;
        @SafeUrl
        private String websiteUrl;
        private Integer displayOrder;
        private Boolean active;
        private MultipartFile logo;
    }

    public record ClientUpdateRequest(@NotBlank @Size(max = 150) String name, @SafeUrl String websiteUrl,
                                      Integer displayOrder, Boolean active) {}
}
