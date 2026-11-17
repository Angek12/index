package com.index.platform.repo;

import com.index.platform.domain.OrgMember;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgMemberRepository extends JpaRepository<OrgMember, Long> {
    Optional<OrgMember> findByUserId(Long userId);
    List<OrgMember> findByOrgTypeAndOrgId(String orgType, Long orgId);
    boolean existsByOrgTypeAndOrgIdAndUserId(String orgType, Long orgId, Long userId);
}
