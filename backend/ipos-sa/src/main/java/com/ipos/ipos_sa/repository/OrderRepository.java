package com.ipos.ipos_sa.repository;

import com.ipos.ipos_sa.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Integer> {
}
