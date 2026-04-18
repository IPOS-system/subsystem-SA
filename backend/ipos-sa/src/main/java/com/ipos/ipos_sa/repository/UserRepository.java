package com.ipos.ipos_sa.repository;

import com.ipos.ipos_sa.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {

  /** Look up credentials at login. */
  Optional<User> findByUsername(String username);

  /** Look up user details when creating/editing accounts. */
  Optional<User> findByUserId(Integer userId);

  /** Enforce unique usernames on account creation. */
  boolean existsByUsername(String username);

  /** List all non-merchant staff. */
  List<User> findByRoleNot(User.Role role);

  /** List all active users of a given role. */
  List<User> findByRoleAndIsActiveTrue(User.Role role);
}
