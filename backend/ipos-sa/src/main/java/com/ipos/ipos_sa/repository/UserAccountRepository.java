package com.ipos.ipos_sa.repository;

import com.ipos.ipos_sa.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Integer> {
}
