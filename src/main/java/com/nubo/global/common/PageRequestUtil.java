package com.nubo.global.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public class PageRequestUtil {

  public static PageRequest of(int page, int size, SortType sort, Class<?> entityClass) {
    String field = sort.getFieldFor(entityClass);
    Sort.Direction dir = (sort == SortType.OLDEST) ? Sort.Direction.ASC : Sort.Direction.DESC;
    if (sort == SortType.ALPHABET) {
      dir = Sort.Direction.ASC;
    }
    return PageRequest.of(page, size, Sort.by(dir, field));
  }

}
