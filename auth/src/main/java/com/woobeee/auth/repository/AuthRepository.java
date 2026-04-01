package com.woobeee.auth.repository;

import com.woobeee.auth.entity.Auth;
import com.woobeee.auth.entity.enums.AuthType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuthRepository extends JpaRepository<Auth, Long> {
    List<Auth> findAllByAuthTypeIn(List<AuthType> authTypes);
    boolean existsByAuthType(AuthType authType);
}
