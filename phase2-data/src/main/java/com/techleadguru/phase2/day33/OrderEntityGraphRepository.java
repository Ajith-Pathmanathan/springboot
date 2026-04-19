package com.techleadguru.phase2.day33;

import com.techleadguru.phase2.shared.Order;
import com.techleadguru.phase2.shared.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Top-level repository for Day33 — extends shared OrderRepository with @EntityGraph methods.
 * Must be top-level for Spring Data JPA to scan it.
 */
@Repository
public interface OrderEntityGraphRepository extends OrderRepository {

    /**
     * Eagerly loads items for all orders — 1 query.
     */
    @EntityGraph(attributePaths = {"items"})
    List<Order> findAll();

    /**
     * Eagerly loads items when finding by userId — still 1 query.
     */
    @EntityGraph(attributePaths = {"items"})
    List<Order> findByUserId(String userId);

    /**
     * KEY ADVANTAGE over JOIN FETCH:
     * findAll with Pageable + @EntityGraph works correctly.
     */
    @EntityGraph(attributePaths = {"items"})
    Page<Order> findAll(Pageable pageable);

    /**
     * findById with @EntityGraph — loads item association eagerly.
     */
    @EntityGraph(attributePaths = {"items"})
    Optional<Order> findWithItemsById(String id);
}
