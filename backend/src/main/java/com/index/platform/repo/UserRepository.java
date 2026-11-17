package com.index.platform.repo;

import com.index.platform.domain.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<User> findByRoleOrderByCreatedAtDesc(Role role);
    List<User> findAllByOrderByCreatedAtDesc();
    List<User> findByApprovalStatusOrderByCreatedAtDesc(ApprovalStatus status);
    long countByApprovalStatus(ApprovalStatus status);
    List<User> findByPhoneIn(Collection<String> phones);
}
