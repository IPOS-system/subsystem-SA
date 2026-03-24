package com.ipos.ipos_sa.repository;

import com.ipos.ipos_sa.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {
}
