package com.shoplab.auth;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthTokenRepository extends JpaRepository<AuthToken, String> {

    /** Load the token together with its user in one query. */
    @EntityGraph(attributePaths = "user")
    Optional<AuthToken> findWithUserByToken(String token);
}
