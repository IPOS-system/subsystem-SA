package com.ipos.ipos_sa.repository;

import com.ipos.ipos_sa.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {
}
