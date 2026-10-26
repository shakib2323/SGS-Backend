package com.sgs.offering;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, Long> {
    List<ServiceItem> findByActiveTrueOrderByDisplayOrderAscIdAsc();
    List<ServiceItem> findAllByOrderByDisplayOrderAscIdAsc();
    Optional<ServiceItem> findBySlugAndActiveTrue(String slug);
    boolean existsBySlug(String slug);
}
