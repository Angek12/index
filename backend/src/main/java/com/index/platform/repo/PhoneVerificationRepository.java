package com.index.platform.repo;

import com.index.platform.domain.PhoneVerification;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification, Long> {
    Optional<PhoneVerification> findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(Long userId);
}
