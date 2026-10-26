package com.sgs.offering;

import com.sgs.common.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

/** A service the company offers (e.g. Spider Glazing). Table: services. Images live in ServiceImage (max 8). */
@Entity
@Table(name = "services")
@Getter
@Setter
@NoArgsConstructor
public class ServiceItem extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, unique = true, length = 160)
    private String slug;

    @Column(name = "short_description", length = 300)
    private String shortDescription;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** e.g. a Font Awesome class such as "fa-border-all", used as a fallback icon. */
    @Column(name = "icon_class", length = 60)
    private String iconClass;

    @Column(length = 150)
    private String warranty;

    /** Free text such as "From Rs 1,200 / sq ft" or "Quote on request". */
    @Column(name = "price_note", length = 120)
    private String priceNote;

    @Column(nullable = false)
    private boolean featured;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "meta_title", length = 160)
    private String metaTitle;

    @Column(name = "meta_description", length = 300)
    private String metaDescription;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "service_features", joinColumns = @JoinColumn(name = "service_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "feature", nullable = false, length = 200)
    @BatchSize(size = 50)
    private List<String> features = new ArrayList<>();
}
