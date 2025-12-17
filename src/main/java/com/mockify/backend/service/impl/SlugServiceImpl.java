package com.mockify.backend.service.impl;

import com.mockify.backend.service.SlugService;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class SlugServiceImpl implements SlugService {

    @Override
    public String generateSlug(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Slug source text cannot be null or blank");
        }

        return text.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    @Override
    public String generateUniqueSlug(String text) {
        String baseSlug = generateSlug(text);
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return baseSlug + "-" + suffix;
    }
}