package com.ipos.ipos_sa.repository;

import com.ipos.ipos_sa.entity.CatalogueItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogueItemRepository extends JpaRepository<CatalogueItem, String> {
}
