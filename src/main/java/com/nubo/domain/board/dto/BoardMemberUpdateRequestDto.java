package com.nubo.domain.board.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoardMemberUpdateRequestDto {

  private List<String> addMemberEmails;
  private List<Long> removeUserIds;
}
