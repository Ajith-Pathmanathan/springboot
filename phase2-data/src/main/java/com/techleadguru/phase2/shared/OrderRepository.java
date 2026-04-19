package com.techleadguru.phase2.shared;

import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

@Primary
public interface OrderRepository extends JpaRepository<Order, String>,
                                          JpaSpecificationExecutor<Order> {

    List<Order> findByUserId(String userId);
    List<Order> findByStatus(String status);

    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.status = :status")
    List<Order> findByUserIdAndStatus(@Param("userId") String userId,
                                      @Param("status") String status);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.userId = :userId")
    long countByUserId(@Param("userId") String userId);

    @Query("SELECT SUM(o.total) FROM Order o WHERE o.userId = :userId")
    BigDecimal sumTotalByUserId(@Param("userId") String userId);

    @Query(value = "SELECT * FROM orders WHERE user_id = :userId AND total > :minTotal ORDER BY total DESC LIMIT :limit",
           nativeQuery = true)
    List<Order> findTopOrdersByUserNative(@Param("userId") String userId,
                                          @Param("minTotal") BigDecimal minTotal,
                                          @Param("limit") int limit);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items")
    List<Order> findAllWithItems();

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items WHERE o.userId = :userId")
    List<Order> findByUserIdWithItems(@Param("userId") String userId);
}
