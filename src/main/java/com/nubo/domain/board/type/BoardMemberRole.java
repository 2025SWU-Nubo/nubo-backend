package com.nubo.domain.board.type;

// 정책: OWNER(보드 소유자), ADMIN(공동관리자)
// 필요 시 VIEWER 등 확장 가능
public enum BoardMemberRole {
  OWNER,
  ADMIN
}
