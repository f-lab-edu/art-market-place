package com.woobeee.auth.repository;

import com.woobeee.auth.entity.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository Layer
 * For User Credential Entity
 * User Credential Entity Only
 * have the user's credential
 * such as password and login id
 */
public interface UserCredentialRepository extends JpaRepository<UserCredential, UUID> {
    Optional<UserCredential> findUserCredentialByLoginId(String loginId);

    Boolean existsByLoginId(String loginId);
}
