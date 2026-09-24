package com.recipebook.repository;

import com.recipebook.model.ShareLink;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ShareLinkRepository extends JpaRepository<ShareLink, Long> {
    Optional<ShareLink> findByToken(String token);
    List<ShareLink> findByRecipeIdAndRevokedFalse(Long recipeId);
}
