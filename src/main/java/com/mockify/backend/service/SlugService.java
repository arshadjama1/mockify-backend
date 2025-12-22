package com.mockify.backend.service;

public interface SlugService {
    String generateSlug(String text);
    String generateUniqueSlug(String text);
}