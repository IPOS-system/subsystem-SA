package com.ipos.ipos_sa.repository;

import com.ipos.ipos_sa.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
}
