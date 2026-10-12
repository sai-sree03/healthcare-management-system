package com.healthcare.hms.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class Paging {

    private static final int MAX_PAGE_SIZE = 100;

    private Paging() {
    }

    /** Builds a Pageable with safe bounds, so ?size=1000000 cannot be abused. */
    public static Pageable of(int page, int size, Sort sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(safePage, safeSize, sort);
    }

    /** Null-safe, trimmed search text ("" matches everything). */
    public static String text(String value) {
        return value == null ? "" : value.trim();
    }
}
