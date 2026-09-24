package com.recipebook.controller;

import com.recipebook.dto.ShareLinkDto;
import com.recipebook.dto.SharedRecipeDto;
import com.recipebook.model.CustomUserDetails;
import com.recipebook.service.ShareLinkService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ShareController {

    private final ShareLinkService shareLinkService;

    public ShareController(ShareLinkService shareLinkService) {
        this.shareLinkService = shareLinkService;
    }

    @GetMapping("/recipes/{id}/share")
    public ResponseEntity<ShareLinkDto> getShareLink(@PathVariable Long id) {
        return shareLinkService.findActiveShareLink(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/recipes/{id}/share")
    public ResponseEntity<ShareLinkDto> createShareLink(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        ShareLinkDto shareLink = shareLinkService.createShareLink(id, userDetails != null ? userDetails.getId() : null);
        return ResponseEntity.status(HttpStatus.CREATED).body(shareLink);
    }

    @DeleteMapping("/recipes/{id}/share")
    public ResponseEntity<Void> revokeShareLink(@PathVariable Long id) {
        shareLinkService.revokeShareLinks(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/share/{token}")
    public ResponseEntity<SharedRecipeDto> getSharedRecipe(@PathVariable String token) {
        return ResponseEntity.ok()
                .header("X-Robots-Tag", "noindex, nofollow")
                .body(shareLinkService.getSharedRecipe(token));
    }
}
