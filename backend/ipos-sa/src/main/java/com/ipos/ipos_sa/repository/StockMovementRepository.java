package com.ipos.ipos_sa.repository;

import com.ipos.ipos_sa.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, Integer> {
}
