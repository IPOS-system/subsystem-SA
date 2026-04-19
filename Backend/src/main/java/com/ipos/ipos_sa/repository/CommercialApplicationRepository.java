package com.ipos.ipos_sa.repository;

import com.ipos.ipos_sa.entity.CommercialApplication;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommercialApplicationRepository
    extends JpaRepository<CommercialApplication, Integer> {

  /** Admin review queue: all applications with a given status. */
  List<CommercialApplication> findByStatusOrderBySubmittedAtDesc(
      CommercialApplication.ApplicationStatus status);

  /** All applications, newest first for the admin management screen. */
  List<CommercialApplication> findAllByOrderBySubmittedAtDesc();
}
