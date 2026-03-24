package com.ipos.ipos_sa.repository;

import com.ipos.ipos_sa.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
}
