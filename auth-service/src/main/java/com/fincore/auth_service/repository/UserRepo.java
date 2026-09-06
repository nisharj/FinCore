package com.fincore.auth_service.repository;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
import com.fincore.auth_service.entity.User;

public interface UserRepo extends JpaRepository<User, Long>{
    Optional<User> findByUsername(String username);
}
