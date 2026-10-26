package com.sgs.contact;

import com.sgs.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "company_contacts")
@Getter
@Setter
@NoArgsConstructor
public class CompanyContact extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_type", nullable = false, length = 30)
    private ContactType type;

    @Column(length = 100)
    private String label;

    @Column(name = "contact_value", nullable = false, length = 500)
    private String value;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active = true;
}
