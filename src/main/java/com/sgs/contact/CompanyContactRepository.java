package com.sgs.contact;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompanyContactRepository extends JpaRepository<CompanyContact, Long> {
    List<CompanyContact> findByActiveTrueOrderByDisplayOrderAscIdAsc();
    List<CompanyContact> findAllByOrderByTypeAscDisplayOrderAscIdAsc();
}
