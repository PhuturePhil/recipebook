package com.recipebook.dto;

public class SourceAuthorDto {

  private String source;
  private String author;

  public SourceAuthorDto(String source, String author) {
    this.source = source;
    this.author = author;
  }

  public String getSource() { return source; }
  public void setSource(String source) { this.source = source; }
  public String getAuthor() { return author; }
  public void setAuthor(String author) { this.author = author; }
}
