package com.sgs.offering;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ServiceImageRepository extends JpaRepository<ServiceImage, Long> {
    List<ServiceImage> findByServiceIdOrderByDisplayOrderAscIdAsc(Long serviceId);
    List<ServiceImage> findByServiceIdInOrderByDisplayOrderAscIdAsc(Collection<Long> serviceIds);
    Optional<ServiceImage> findByIdAndServiceId(Long id, Long serviceId);
    long countByServiceId(Long serviceId);
}
