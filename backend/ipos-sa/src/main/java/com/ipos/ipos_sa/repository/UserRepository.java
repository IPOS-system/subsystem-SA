package com.ipos.ipos_sa.repository;

import com.ipos.ipos_sa.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {

  /** Used by AuthService to look up credentials at login. */
  Optional<User> findByUsername(String username);

  /** Used by AccountService to look up user details when creating/editing accounts. */
  Optional<User> findByUserId(Integer userId);

  /** Used by AccountService to enforce unique usernames on account creation. */
  boolean existsByUsername(String username);

  /** Used by Admin account management screen to list all non-merchant staff. */
  List<User> findByRoleNot(User.Role role);

  /** Used to list all active users of a given role (e.g. all active MANAGERs). */
  List<User> findByRoleAndIsActiveTrue(User.Role role);
}
