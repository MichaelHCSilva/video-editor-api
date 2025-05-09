package com.l8group.videoeditor.repositories;

import com.l8group.videoeditor.enums.VideoStatusEnum;
import com.l8group.videoeditor.models.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserAccount, UUID> {

    Optional<UserAccount> findByUserName(String userName);
    Optional<UserAccount> findByEmail(String email);
    boolean existsByUserName(String userName);
    boolean existsByEmail(String email);

    List<UserAccount> findByStatus(VideoStatusEnum status);
}