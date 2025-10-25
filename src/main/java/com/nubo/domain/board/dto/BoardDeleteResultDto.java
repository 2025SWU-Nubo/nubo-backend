package com.nubo.domain.board.dto;

import com.nubo.domain.card.dto.CardRestoreRequestDto;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardDeleteResultDto {

  private Long boardId;          // 보드 ID
  private String status;         // 처리 상태 (HIDDEN 기본보드 | DELETED 사용자보드 | FAILED)
  private String option;         // 요청 옵션 (DETACH_ONLY | DELETE_ORPHANS)
  private int linksDetached;     // 끊어진 보드-카드 링크 수
  private int cardsSoftDeleted;  // soft delete된 카드 수 (DELETE_ORPHANS 옵션 시)
  private int sectionsDeleted;   // 삭제된 섹션 수 (사용자보드 트리 기준)
  private String error;          // 실패 시 에러코드 (ACCESS_DENIED, ENTITY_NOT_FOUND 등)
  private List<Long> deletedSectionIds; // 삭제된 섹션 ID 목록
  private List<CardRestoreRequestDto> cardRestores;
}