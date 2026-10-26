package com.sgs.client;

import com.sgs.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor
public class Client extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "logo_path", nullable = false, length = 255)
    private String logoPath;

    @Column(name = "thumb_path", nullable = false, length = 255)
    private String thumbPath;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active = true;
}
