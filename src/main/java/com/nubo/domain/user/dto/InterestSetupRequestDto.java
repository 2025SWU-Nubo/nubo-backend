package com.nubo.domain.user.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InterestSetupRequestDto {

  private List<Long> selectedBoardIds; // 사용자가 선택한 보드 ID들

  @NotNull
  private boolean skip; // 건너뛰기 여부
}
