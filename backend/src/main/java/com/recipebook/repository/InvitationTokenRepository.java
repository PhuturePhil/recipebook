package com.recipebook.repository;

import com.recipebook.model.InvitationToken;
import com.recipebook.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InvitationTokenRepository extends JpaRepository<InvitationToken, Long> {
    Optional<InvitationToken> findByToken(String token);
    void deleteByInvitedBy(User invitedBy);
}
