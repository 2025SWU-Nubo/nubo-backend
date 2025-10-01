package com.nubo.domain.board.type;

public enum InvitationStatus {
  PENDING,   // 초대 발송 직후
  ACCEPTED,  // 수락됨 → BoardMember 생성
  REJECTED   // 거절됨
}