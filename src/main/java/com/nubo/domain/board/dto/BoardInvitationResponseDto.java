package com.nubo.domain.board.dto;

import com.nubo.domain.board.type.InvitationStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BoardInvitationResponseDto {

  private Long invitationId;
  private String email;
  private String nickname;
  private InvitationStatus status; // PENDING / ACCEPTED / REJECTED
}
