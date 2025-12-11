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
      case LATEST -> Sort.Direction.DESC;
      case OLDEST, ALPHABET -> Sort.Direction.ASC;
    };

    // 기본 정렬 조건
    Sort.Order primary = new Sort.Order(dir, field);

    // 보조 정렬 조건 (lastCardAddedAt이 null일 때 updatedAt으로 정렬)
    if (field.endsWith("lastCardAddedAt")) {
      String secondaryField = prefix != null ? prefix + "updatedAt" : "updatedAt";
      Sort.Order secondary = new Sort.Order(dir, secondaryField);
      return PageRequest.of(page, size, Sort.by(primary, secondary));
    }

    return PageRequest.of(page, size, Sort.by(primary));
  }
}
