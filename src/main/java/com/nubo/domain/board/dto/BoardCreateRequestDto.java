package com.nubo.domain.board.dto;

import com.nubo.domain.board.type.BoardType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;

@Getter
public class BoardCreateRequestDto {

  @NotBlank
  private String name;

  @NotNull
  private BoardType boardType; // SECTION or BOARD

  private Long parentBoardId; // 섹션인 경우 필요

  // 공유보드 여부 (BOARD일 때만 의미 있음)
  private boolean shared;

  // ✅ 공유보드일 때 초대할 사용자 이메일 목록 (owner 본인은 제외됨)
  // 비어 있어도 됨(공유 on 만 하고 나중에 초대 추가 가능)
  private List<@Email String> memberEmails;
}
