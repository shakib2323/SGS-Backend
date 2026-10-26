package com.sgs.project;

import com.sgs.category.Category;
import com.sgs.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
public class Project extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(length = 150)
    private String location;

    @Column(name = "client_name", length = 150)
    private String clientName;

    @Column(name = "started_year", nullable = false)
    private int startedYear;

    @Column(name = "completed_year")
    private Integer completedYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectStatus status;

    @Column(name = "area_sq_ft")
    private Integer areaSqFt;

    @Column(nullable = false)
    private boolean featured;

    @Column(name = "image_path", nullable = false, length = 255)
    private String imagePath;

    @Column(name = "thumb_path", nullable = false, length = 255)
    private String thumbPath;
}
