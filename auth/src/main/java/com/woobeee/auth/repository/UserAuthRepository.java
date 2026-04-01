package com.woobeee.auth.repository;

import com.woobeee.auth.entity.UserAuth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserAuthRepository extends JpaRepository<UserAuth, UserAuth.UserAuthId> {
    List<UserAuth> findAllById_UserId(UUID idUserId);
}
