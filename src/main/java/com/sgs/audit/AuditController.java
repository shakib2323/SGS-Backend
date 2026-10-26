package com.sgs.audit;

import com.sgs.common.PageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/admin/audit")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogRepository repo;

    public record AuditDto(Long id, Long actorId, String actorUsername, String action,
                           String entityType, Long entityId, String details, Instant createdAt) {}

    @GetMapping
    public PageDto<AuditDto> list(@RequestParam(name = "entityType", required = false) String entityType,
                                  @RequestParam(name = "page", defaultValue = "0") int page,
                                  @RequestParam(name = "size", defaultValue = "50") int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200));
        Page<AuditLog> p = (entityType == null || entityType.isBlank())
                ? repo.findAllByOrderByIdDesc(pr)
                : repo.findByEntityTypeOrderByIdDesc(entityType.trim(), pr);
        return PageDto.of(p.map(a -> new AuditDto(a.getId(), a.getActorId(), a.getActorUsername(), a.getAction(),
                a.getEntityType(), a.getEntityId(), a.getDetails(), a.getCreatedAt())));
    }
}
