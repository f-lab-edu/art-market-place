package com.woobeee.artmarketplace.auth.repository;

import java.util.Optional;

import com.woobeee.artmarketplace.auth.entity.Seller;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerRepository extends JpaRepository<Seller, Long> {
    boolean existsByGoogleSubject(String googleSubject);

    Optional<Seller> findByGoogleSubject(String googleSubject);
}
