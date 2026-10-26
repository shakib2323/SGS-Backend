package com.sgs.gallery;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface GalleryRepository extends JpaRepository<GalleryImage, Long>, JpaSpecificationExecutor<GalleryImage> {

    @Override
    @EntityGraph(attributePaths = {"category", "uploadedBy"})
    Page<GalleryImage> findAll(Specification<GalleryImage> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"category", "uploadedBy"})
    Optional<GalleryImage> findById(Long id);

    boolean existsByCategoryId(Long categoryId);
}
