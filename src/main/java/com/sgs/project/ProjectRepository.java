package com.sgs.project;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {

    @Override
    @EntityGraph(attributePaths = {"category"})
    Page<Project> findAll(Specification<Project> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"category"})
    Optional<Project> findById(Long id);

    boolean existsByCategoryId(Long categoryId);
}
