package com.jauharxlr.springbase.auth.repository;

import com.jauharxlr.springbase.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailAndProjectRef(String email, String projectRef);
}
