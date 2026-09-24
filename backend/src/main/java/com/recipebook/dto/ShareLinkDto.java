package com.recipebook.dto;

import java.time.LocalDateTime;

public class ShareLinkDto {

  private String token;
  private String url;
  private LocalDateTime createdAt;
  private LocalDateTime expiresAt;

  public ShareLinkDto(String token, String url, LocalDateTime createdAt, LocalDateTime expiresAt) {
    this.token = token;
    this.url = url;
    this.createdAt = createdAt;
    this.expiresAt = expiresAt;
  }

  public String getToken() { return token; }
  public String getUrl() { return url; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public LocalDateTime getExpiresAt() { return expiresAt; }
}
