package com.nubo.global.common;

public enum SortType {
  LATEST,    // 최신순
  OLDEST,    // 오래된순
  ALPHABET;  // 가나다순

  public String getFieldFor(Class<?> entityClass) {
    if (this == ALPHABET) {
      return entityClass.getSimpleName().equals("Card") ? "title" : "name";
    }
    return "createdAt";
  }
}
