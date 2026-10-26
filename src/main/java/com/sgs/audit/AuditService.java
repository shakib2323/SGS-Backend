package com.sgs.audit;

import com.sgs.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** Writes inside the caller's transaction, so an action and its audit row commit (or roll back) together. */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repo;
    private final Clock clock;

    @Transactional
    public void log(AuthUser actor, String action, String entityType, Long entityId, String details) {
        AuditLog a = new AuditLog();
        a.setActorId(actor == null ? null : actor.id());
        a.setActorUsername(actor == null ? null : actor.username());
        a.setAction(action);
        a.setEntityType(entityType);
        a.setEntityId(entityId);
        a.setDetails(details == null ? null : (details.length() > 1000 ? details.substring(0, 1000) : details));
        a.setCreatedAt(clock.instant());
        repo.save(a);
    }
}
