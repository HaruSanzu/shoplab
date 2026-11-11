package com.shoplab.common;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** One page of a list. Page numbers start at 1, like in the frontend URL (?page=2). */
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalItems,
        int totalPages
) {
    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber() + 1, page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
