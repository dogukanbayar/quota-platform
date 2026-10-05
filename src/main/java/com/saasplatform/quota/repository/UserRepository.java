package com.saasplatform.quota.repository;

import com.saasplatform.quota.entity.AppUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<AppUser, Long> {

    boolean existsByEmail(String email);

    @Query("select u from AppUser u left join fetch u.subscription s left join fetch s.plan order by u.id")
    List<AppUser> findAllWithSubscription();

    @Query("select u from AppUser u left join fetch u.subscription s left join fetch s.plan where u.id = :id")
    Optional<AppUser> findByIdWithSubscription(@Param("id") Long id);
}
