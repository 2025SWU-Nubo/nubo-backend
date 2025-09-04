package com.nubo.domain.board.mapper;

import com.nubo.domain.board.dto.BoardMemberResponseDto;
import com.nubo.domain.board.entity.Board;
import com.nubo.domain.board.entity.BoardMember;
import com.nubo.domain.board.type.BoardMemberRole;
import com.nubo.domain.user.entity.User;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BoardMemberMapper {

  // OWNER 1명 생성
  public BoardMember toOwner(Board board, User owner) {
    return BoardMember.builder()
      .board(board)
      .user(owner)
      .role(BoardMemberRole.OWNER)
      .build();
  }

  // ADMIN 여러 명 생성
  public List<BoardMember> toAdmins(Board board, List<User> admins) {
    List<BoardMember> list = new ArrayList<>();
    if (admins == null || admins.isEmpty()) {
      return list;
    }
    for (User u : admins) {
      list.add(BoardMember.builder()
        .board(board)
        .user(u)
        .role(BoardMemberRole.ADMIN)
        .build());
    }
    return list;
  }

  // OWNER + ADMINs 한번에
  public List<BoardMember> toOwnerAndAdmins(Board board, User owner, List<User> admins) {
    List<BoardMember> all = new ArrayList<>();
    all.add(toOwner(board, owner));
    all.addAll(toAdmins(board, admins));
    return all;
  }

  // Entity → DTO 변환
  public BoardMemberResponseDto toResponseDto(BoardMember member) {
    return BoardMemberResponseDto.builder()
      .userId(member.getUser().getId())
      .nickname(member.getUser().getNickname())
      .build();
  }
}
