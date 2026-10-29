package com.index.platform.repo;

import com.index.platform.domain.*;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface FarmSupplyRepository extends JpaRepository<FarmSupply, Long> {
    List<FarmSupply> findByFarmerIdOrderByCreatedAtDesc(Long farmerId);
    List<FarmSupply> findByStatusOrderByCreatedAtDesc(SupplyStatus status);
    List<FarmSupply> findAllByOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from FarmSupply s where s.id = :id")
    Optional<FarmSupply> findByIdForUpdate(@Param("id") Long id);

    @Query("select s from FarmSupply s where s.crop.id = :cropId and s.status = :status and s.quantityAvailable > 0 "
         + "and s.availableFrom <= :date and (s.availableUntil is null or s.availableUntil >= :date) "
         + "order by s.pricePerUnit asc")
    List<FarmSupply> findMatchingSupplies(@Param("cropId") Long cropId, @Param("status") SupplyStatus status, @Param("date") LocalDate date);
}
