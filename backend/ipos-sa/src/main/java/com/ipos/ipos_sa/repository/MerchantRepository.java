package com.ipos.ipos_sa.repository;

import com.ipos.ipos_sa.entity.Merchant;
import com.ipos.ipos_sa.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MerchantRepository extends JpaRepository<Merchant, Integer> {

    /** Look up the merchant record that belongs to a given user account. */
    Optional<Merchant> findByUser_UserId(Integer userId);

    /** Look up the merchant record by User entity. */
    Optional<Merchant> findByUser(User user);

    /** Used by Admin/Manager screens to filter merchants by account state. */
    List<Merchant> findByAccountStatus(Merchant.AccountStatus accountStatus);

    /** Used by Manager dashboard notification: all merchants currently IN_DEFAULT. */
    List<Merchant> findByAccountStatusNot(Merchant.AccountStatus accountStatus);

    /**
     * Search across company name or email — powers the searchable list on
     * the Manage Accounts screen.
     */
    @Query("SELECT m FROM Merchant m WHERE " +
           "LOWER(m.companyName) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
           "LOWER(m.email)       LIKE LOWER(CONCAT('%', :term, '%'))")
    List<Merchant> searchByNameOrEmail(@Param("term") String term);
}