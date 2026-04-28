package com.mockify.backend.common.validation;

import com.mockify.backend.exception.BadRequestException;
import org.springframework.data.domain.Pageable;

/**
 * Utility class to validate pagination parameters received from client requests.
 *
 * Ensures:
 * - Page number is not negative
 * - Page size is greater than 0
 * - Page size does not exceed the allowed maximum limit
 *
 * Helps prevent invalid or excessive pagination inputs that could impact performance
 * or cause unexpected behavior in data retrieval.
 *
 * Two validation methods are provided:
 * - Default validation using predefined MAX_SIZE
 * - Custom validation allowing dynamic max page size
 */

public class PageableValidator {

    private static final int MAX_SIZE = 100;
    private static final int MIN_PAGE = 0;

    public static void validate(Pageable pageable) {

        if (pageable.getPageNumber() < MIN_PAGE) {
            throw new BadRequestException("Page number cannot be negative");
        }

        if (pageable.getPageSize() > MAX_SIZE) {
            throw new BadRequestException("Page size cannot exceed " + MAX_SIZE);
        }

        if (pageable.getPageSize() <= 0) {
            throw new BadRequestException("Page size must be greater than 0");
        }
    }

    public static void validate(Pageable pageable, int maxSize) {

        if (pageable.getPageNumber() < MIN_PAGE) {
            throw new BadRequestException("Page number cannot be negative");
        }

        if (pageable.getPageSize() > maxSize) {
            throw new BadRequestException("Page size cannot exceed " + MAX_SIZE);
        }

        if (pageable.getPageSize() <= 0) {
            throw new BadRequestException("Page size must be greater than 0");
        }
    }
}
