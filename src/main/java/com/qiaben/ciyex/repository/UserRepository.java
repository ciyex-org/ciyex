package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);


    boolean existsByEmail(String email);

    void deleteByEmail(String email);
}
