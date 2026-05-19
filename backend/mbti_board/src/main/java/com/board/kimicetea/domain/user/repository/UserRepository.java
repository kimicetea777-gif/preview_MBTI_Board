package com.board.kimicetea.domain.user.repository;

import com.board.kimicetea.domain.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    Optional<User> findBySocialProviderAndProviderId(String socialProvider, String providerId);

    @EntityGraph(attributePaths = { "mbti" })
    Optional<User> findWithMbtiById(UUID id);
}
