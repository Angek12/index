package com.index.platform.repo;

import com.index.platform.domain.PasswordReset;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {
    Optional<PasswordReset> findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(Long userId);
}
