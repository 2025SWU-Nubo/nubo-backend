package com.nubo.domain.board.dto;

import java.util.List;
import lombok.Getter;

@Getter
public class BoardRestoreRequestDto {

  private List<Long> boardIds;
}
