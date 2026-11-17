package com.index.platform.service;

import com.index.platform.domain.AuditLog;
import com.index.platform.dto.Dto;
import com.index.platform.repo.AuditLogRepository;
import com.index.platform.repo.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Keeps a record of important admin actions. */
@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository repo;
    private final UserRepository users;

    @Transactional
    public void log(Long actorId, String action, String targetType, Long targetId, String details) {
        AuditLog a = new AuditLog();
        a.setActor(actorId == null ? null : users.getReferenceById(actorId));
        a.setAction(action);
        a.setTargetType(targetType);
        a.setTargetId(targetId);
        a.setDetails(details == null ? null : (details.length() > 500 ? details.substring(0, 500) : details));
        repo.save(a);
    }

    @Transactional(readOnly = true)
    public List<Dto.AuditEntry> latest() {
        return repo.findTop200ByOrderByCreatedAtDesc().stream()
                .map(a -> new Dto.AuditEntry(a.getId(), a.getActor() == null ? null : a.getActor().getFullName(),
                        a.getAction(), a.getTargetType(), a.getTargetId(), a.getDetails(), a.getCreatedAt()))
                .toList();
    }
}
