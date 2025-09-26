package com.nubo.global.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public class PageRequestUtil {

  public static PageRequest of(int page, int size, SortType sort) {
    Sort sortOption = switch (sort) {
      case OLDEST -> Sort.by("createdAt").ascending();
      case ALPHABET -> Sort.by("title").ascending();
      default -> Sort.by("createdAt").descending();
    };
    return PageRequest.of(page, size, sortOption);
  }
}
