package com.nullers.restbookstore.pagination.models;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Clase PageResponse
 *
 * @param <T> Tipo de dato
 */
public record PageResponse<T>(
        List<T> content,
        int totalPages,
        long totalElements,
        int pageSize,
        int pageNumber,
        int totalPageElements,
        boolean empty,
        boolean first,
        boolean last,
        String orderBy,
        String direction
) {
    /**
     * PageResponse
     *
     * @param page      Página
     * @param orderBy    Ordenación
     * @param direction Dirección
     * @param <T>       Tipo de dato
     * @return PageResponse
     */
    public static <T> PageResponse<T> of(Page<T> page, String orderBy, String direction) {
        return new PageResponse<>(
                page.getContent(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.getSize(),
                page.getNumber(),
                page.getNumberOfElements(),
                page.isEmpty(),
                page.isFirst(),
                page.isLast(),
                orderBy,
                direction
        );
    }
}