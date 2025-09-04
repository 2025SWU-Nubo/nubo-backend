package com.nubo.domain.board.service;

import com.nubo.domain.board.dto.BoardMemberListResponseDto;
import com.nubo.domain.board.dto.BoardMemberResponseDto;
import com.nubo.domain.board.dto.BoardMemberUpdateRequestDto;
import com.nubo.domain.board.entity.Board;
import com.nubo.domain.board.entity.BoardMember;
import com.nubo.domain.board.mapper.BoardMemberMapper;
import com.nubo.domain.board.repository.BoardMemberRepository;
import com.nubo.domain.board.type.BoardMemberRole;
import com.nubo.domain.user.entity.User;
import com.nubo.domain.user.service.UserService;
import com.nubo.global.error.ErrorCode;
import com.nubo.global.error.exception.ApiException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BoardMemberService {

  private final BoardMemberRepository boardMemberRepository;
  private final BoardMemberMapper boardMemberMapper;
  private final UserService userService;

  /**
   * OWNER와 ADMIN 멤버를 생성하여 저장한다.
   */
  @Transactional
  public void createOwnerAndAdmins(Board savedBoard, User owner, List<User> admins) {
    List<BoardMember> members = admins.isEmpty()
      ? List.of(boardMemberMapper.toOwner(savedBoard, owner))
      : boardMemberMapper.toOwnerAndAdmins(savedBoard, owner, admins);

    boardMemberRepository.saveAll(members);
  }

  /**
   * 사용자와 보드 목록 기준으로 즐겨찾기 상태 맵을 조회한다.
   */
  @Transactional(readOnly = true)
  public Map<Long, Boolean> getFavoriteMapByUserAndBoardIds(Long userId, List<Long> boardIds) {
    return boardMemberRepository.findByUserIdAndBoardIds(userId, boardIds).stream()
      .collect(Collectors.toMap(
        bm -> bm.getBoard().getId(),
        BoardMember::isFavorite
      ));
  }

  /**
   * 특정 보드에서 사용자의 즐겨찾기 상태를 조회한다.
   */
  @Transactional(readOnly = true)
  public boolean getFavoriteStatus(Long boardId, Long userId) {
    return getMemberOrThrow(boardId, userId).isFavorite();
  }

  /**
   * 즐겨찾기 상태를 업데이트한다.
   */
  @Transactional
  public boolean updateFavorite(Long userId, Long boardId, boolean favorite) {
    BoardMember member = getMemberOrThrow(boardId, userId);
    member.updateFavorite(favorite);
    boardMemberRepository.save(member);
    return member.isFavorite();
  }

  /**
   * 보드 멤버 목록을 갱신한다. (추가/제거)
   */
  @Transactional
  public BoardMemberListResponseDto updateMembers(Board board, BoardMemberUpdateRequestDto dto) {

    // --- 멤버 추가 ---
    if (dto.getAddMemberEmails() != null && !dto.getAddMemberEmails().isEmpty()) {
      List<User> invitees = userService.getUsersByEmails(dto.getAddMemberEmails());

      for (User invitee : invitees) {
        boolean exists = boardMemberRepository.existsByBoard_IdAndUser_Id(board.getId(),
          invitee.getId());
        if (!exists) {
          BoardMember member = BoardMember.builder()
            .board(board)
            .user(invitee)
            .role(BoardMemberRole.ADMIN) // 정책상 모두 관리자
            .build();

          boardMemberRepository.save(member);
        }
      }
    }

    // --- 멤버 제거 ---
    if (dto.getRemoveUserIds() != null && !dto.getRemoveUserIds().isEmpty()) {
      for (Long userId : dto.getRemoveUserIds()) {
        boardMemberRepository.findByBoard_IdAndUser_Id(board.getId(), userId)
          .ifPresent(boardMemberRepository::delete);
      }
    }

    // 최종 멤버 목록 조회
    List<BoardMember> members = boardMemberRepository.findAllByBoardId(board.getId());
    List<BoardMemberResponseDto> memberDtos = members.stream()
      .map(boardMemberMapper::toResponseDto)
      .toList();

    return BoardMemberListResponseDto.builder()
      .boardId(board.getId())
      .members(memberDtos)
      .build();
  }

  /**
   * 보드에 사용자가 속해 있는지 여부를 확인한다.
   */
  @Transactional(readOnly = true)
  public boolean existsByBoardAndUser(Long boardId, Long userId) {
    return boardMemberRepository.existsByBoard_IdAndUser_Id(boardId, userId);
  }

  /**
   * 보드 ID 목록 기준으로 모든 멤버를 삭제한다.
   */
  @Transactional
  public void deleteByBoardIds(List<Long> boardIds) {
    boardMemberRepository.deleteByBoardIds(boardIds);
  }

  // ====== private helper ======

  /**
   * 보드 멤버를 조회하거나 없으면 예외를 던진다.
   */
  private BoardMember getMemberOrThrow(Long boardId, Long userId) {
    return boardMemberRepository.findByBoard_IdAndUser_Id(boardId, userId)
      .orElseThrow(() -> new ApiException(ErrorCode.ACCESS_DENIED));
  }
}
