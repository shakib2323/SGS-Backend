package com.sgs.contact;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ContactDtos {
    private ContactDtos() {}

    public record ContactDto(Long id, ContactType type, String label, String value, int displayOrder, boolean active) {}

    /** Examples: PHONE "+91 81254 43453"; EMAIL "contact@..."; ADDRESS "Sy No 3, ..."; WHATSAPP "+91 63970 62557". */
    public record ContactRequest(@NotNull ContactType type,
                                 @Size(max = 100) String label,
                                 @NotBlank @Size(max = 500) String value,
                                 Integer displayOrder,
                                 Boolean active) {}
}
