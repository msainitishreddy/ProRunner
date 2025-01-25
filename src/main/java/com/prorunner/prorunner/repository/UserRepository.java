package com.prorunner.prorunner.repository;

import com.prorunner.prorunner.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);


    @EntityGraph(attributePaths = "roles") // Eagerly load roles
    Optional<User> findByUsername(String username);
}
