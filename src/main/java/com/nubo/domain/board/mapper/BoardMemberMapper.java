package com.nubo.domain.board.mapper;

import com.nubo.domain.board.dto.BoardInvitationResponseDto;
import com.nubo.domain.board.dto.BoardMemberListResponseDto;
import com.nubo.domain.board.dto.BoardMemberResponseDto;
import com.nubo.domain.board.entity.Board;
import com.nubo.domain.board.entity.BoardInvitation;
import com.nubo.domain.board.entity.BoardMember;
import com.nubo.domain.board.type.BoardMemberRole;
import com.nubo.domain.board.type.InvitationStatus;
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
      .role(member.getRole())
      .build();
  }

  /**
   * 멤버 + 초대 목록을 포함한 응답 DTO 생성
   */
  public BoardMemberListResponseDto toMemberListResponseDto(
    Board board,
    List<BoardMember> members,
    List<BoardInvitation> invitations
  ) {
    List<BoardMemberResponseDto> memberDtos = members.stream()
      .map(this::toResponseDto)
      .toList();

    List<BoardInvitationResponseDto> invitationDtos = invitations.stream()
      .filter(inv -> inv.getStatus() == InvitationStatus.PENDING)
      .map(invite -> BoardInvitationResponseDto.builder()
        .invitationId(invite.getId())
        .email(invite.getInvitee().getEmail())
        .nickname(invite.getInvitee().getNickname())
        .status(invite.getStatus())
        .build())
      .toList();

    return BoardMemberListResponseDto.builder()
      .boardId(board.getId())
      .members(memberDtos)
      .invitations(invitationDtos)
      .build();
  }

  /**
   * 기본 보드에 대한 BoardMember 리스트 생성
   */
  public List<BoardMember> toDefaultBoardMembers(List<Board> boards, User user) {
    return boards.stream()
      .map(board -> BoardMember.builder()
        .board(board)
        .user(user)
        .role(BoardMemberRole.OWNER)
        .favorite(false)
        .visible(false)
        .build())
      .toList();
  }
}
