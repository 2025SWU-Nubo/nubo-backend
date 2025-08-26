// src/main/java/com/nubo/domain/board/dto/BoardDeleteResultDto.java
package com.nubo.domain.board.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardDeleteResultDto {

  private Long boardId;

  /**
   * HIDDEN(기본보드), DELETED(사용자보드), FAILED
   */
  private String status;

  /**
   * DETACH_ONLY | DELETE_ORPHANS (요청 옵션 에코)
   */
  private String option;

  /**
   * 해당 보드 트리에서 끊은 링크 수
   */
  private int linksDetached;

  /**
   * 옵션이 DELETE_ORPHANS일 때 soft delete된 카드 수 (기본보드도 포함)
   */
  private int cardsSoftDeleted;

  /**
   * 삭제된 섹션 개수(사용자보드 트리에서만 의미)
   */
  private int sectionsDeleted;

  /**
   * 실패 시 에러코드 문자열 (예: ACCESS_DENIED, ENTITY_NOT_FOUND)
   */
  private String error;
}