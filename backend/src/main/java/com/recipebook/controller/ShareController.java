package com.recipebook.controller;

import com.recipebook.dto.ShareLinkDto;
import com.recipebook.dto.SharedRecipeDto;
import com.recipebook.model.CustomUserDetails;
import com.recipebook.service.ShareLinkService;
import com.recipebook.translation.RecipeLanguage;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
    public ResponseEntity<SharedRecipeDto> getSharedRecipe(@PathVariable String token,
                                                           @RequestParam(name = "lang", required = false) String lang) {
        if (lang != null && !RecipeLanguage.isSupported(lang)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unbekannte Sprache.");
        }
        return ResponseEntity.ok()
                .header("X-Robots-Tag", "noindex, nofollow")
                .body(lang == null ? shareLinkService.getSharedRecipe(token) : shareLinkService.getSharedRecipe(token, lang));
    }

    @GetMapping("/share/{token}/image")
    public ResponseEntity<byte[]> getSharedImage(@PathVariable String token) {
        return ImageResponses.of(shareLinkService.getSharedImageUrl(token), CacheControl.noCache().cachePublic());
    }
}
