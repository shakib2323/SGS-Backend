package com.sgs.tool;

import com.sgs.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tools")
@Getter
@Setter
@NoArgsConstructor
public class Tool extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "tool_link", length = 500)
    private String toolLink;

    @Column(name = "image_path", nullable = false, length = 255)
    private String imagePath;

    @Column(name = "thumb_path", nullable = false, length = 255)
    private String thumbPath;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active = true;
}
