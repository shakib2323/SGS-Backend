package com.sgs.offering;

import com.sgs.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "service_images")
@Getter
@Setter
@NoArgsConstructor
public class ServiceImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceItem service;

    @Column(name = "image_path", nullable = false, length = 255)
    private String imagePath;

    @Column(name = "thumb_path", nullable = false, length = 255)
    private String thumbPath;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
