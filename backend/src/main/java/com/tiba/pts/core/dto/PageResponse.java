package com.tiba.pts.core.dto;

import org.springframework.data.domain.Page;
import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(
    List<T> content,
    int pageNumber,
    int pageSize,
    long totalElements,
    int totalPages,
    boolean isLast) {

  public static <E, D> PageResponse<D> of(Page<E> page, Function<E, D> mapper) {
    List<D> mappedContent = page.getContent().stream().map(mapper).toList();
    return new PageResponse<>(
        mappedContent,
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.isLast());
  }
}
