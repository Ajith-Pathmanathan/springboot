package com.techleadguru.phase2.day35;

import com.techleadguru.phase2.shared.Order;
import com.techleadguru.phase2.shared.OrderRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Top-level repository for Day35 — adds JPQL aggregate queries not in shared OrderRepository.
 * Must be top-level for Spring Data JPA component scanning.
 */
@Repository
public interface OrderJpqlRepository extends OrderRepository {

    // JPQL — static complex query (re-declared for demo; also available via findByUserIdAndStatus in shared)
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.status = :status")
    List<Order> findByUserIdAndStatusJpql(@Param("userId") String userId,
                                          @Param("status") String status);

    // JPQL aggregate
    @Query("SELECT COUNT(o) FROM Order o WHERE o.userId = :userId")
    long countByUserIdJpql(@Param("userId") String userId);

    @Query("SELECT SUM(o.total) FROM Order o WHERE o.userId = :userId")
    BigDecimal sumTotalByUserId(@Param("userId") String userId);

    // Native SQL — already in shared.OrderRepository as findTopOrdersByUserNative
    // Redeclared here for Day35 demo with explicit name
    @Query(value = "SELECT * FROM orders WHERE user_id = :userId AND total > :minTotal ORDER BY total DESC LIMIT :limit",
           nativeQuery = true)
    List<Order> findTopOrdersByUserIdNative(@Param("userId") String userId,
                                             @Param("minTotal") BigDecimal minTotal,
                                             @Param("limit") int limit);
}
