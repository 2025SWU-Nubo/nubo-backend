package com.nubo.global.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public class PageRequestUtil {

  public static PageRequest of(int page, int size, SortType sort, Class<?> entityClass) {
    return of(page, size, sort, entityClass, null);
  }

  public static PageRequest of(int page, int size, SortType sort, Class<?> entityClass,
    String prefix) {
    String field = sort.getFieldFor(entityClass);

    if (prefix != null && !field.startsWith(prefix)) {
      field = prefix + field;
    }

    Sort.Direction dir = switch (sort) {
      case OLDEST -> Sort.Direction.ASC;
      case ALPHABET -> Sort.Direction.ASC;
      default -> Sort.Direction.DESC;
    };

    return PageRequest.of(page, size, Sort.by(dir, field));
  }
}
