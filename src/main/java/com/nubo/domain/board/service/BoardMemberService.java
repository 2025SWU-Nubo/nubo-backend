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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BoardMemberService {

  private final BoardMemberRepository boardMemberRepository;
  private final BoardMemberMapper boardMemberMapper;
  private final UserService userService;

  /**
   * 특정 보드의 멤버 전체 조회
   */
  public List<BoardMember> getMembers(Board board) {
    return boardMemberRepository.findAllByBoardIdOrderByRoleAndCreatedAt(board.getId());
  }

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
    List<BoardMember> members = boardMemberRepository.findActiveMembersByBoardId(board.getId());
    List<BoardMemberResponseDto> memberDtos = members.stream()
      .map(boardMemberMapper::toResponseDto)
      .toList();

    return BoardMemberListResponseDto.builder()
      .boardId(board.getId())
      .members(memberDtos)
      .build();
  }

  // OWNER 자격을 생성한다.
  public void createOwner(Board board, User owner) {
    BoardMember ownerMember = BoardMember.builder()
      .board(board)
      .user(owner)
      .role(BoardMemberRole.OWNER)
      .build();
    boardMemberRepository.save(ownerMember);
  }

  // ADMIN 자격을 생성한다.
  @Transactional
  public void createAdmins(Board board, List<User> admins) {
    for (User admin : admins) {
      BoardMember adminMember = BoardMember.builder()
        .board(board)
        .user(admin)
        .role(BoardMemberRole.ADMIN)
        .build();
      boardMemberRepository.save(adminMember);
    }
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

  /**
   * 사용자가 최근 방문한 보드 목록을 조회한다.
   */
  @Transactional(readOnly = true)
  public List<BoardMember> findRecentVisitedBoards(Long userId, int limit) {
    return boardMemberRepository.findRecentVisitedBoards(userId, PageRequest.of(0, limit));
  }

  /**
   * 보드의 방문 시간을 업데이트한다.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void updateLastVisitedAt(Long boardId, Long userId) {
    boardMemberRepository.updateLastVisitedAt(boardId, userId, LocalDateTime.now());
  }

  /**
   * 특정 사용자의 AI 기본보드를 숨김 처리한다.
   */
  @Transactional
  public void hideBoardForUser(Long boardId, Long userId) {
    int updated = boardMemberRepository.updateVisibleFalse(boardId, userId);
    if (updated == 0) {
      // 멤버십이 없으면 새로 생성 후 숨김 처리 (초기 관심사 미설정 유저용)
      BoardMember newMember = BoardMember.builder()
        .board(Board.builder().id(boardId).build())
        .user(User.builder().id(userId).build())
        .visible(false)
        .build();
      boardMemberRepository.save(newMember);
    }
  }

  /**
   * 특정 사용자의 AI 기본보드를 복원한다.
   */
  @Transactional
  public void restoreVisibleForUser(Long boardId, Long userId) {
    int updated = boardMemberRepository.updateVisibleTrue(boardId, userId);
    if (updated == 0) {
      // 혹시 숨김 처리된 적이 없던 경우, 새 멤버십 생성 + visible=true로 복원
      BoardMember newMember = BoardMember.builder()
        .board(Board.builder().id(boardId).build())
        .user(User.builder().id(userId).build())
        .visible(true)
        .build();
      boardMemberRepository.save(newMember);
    }
  }

  /**
   * 특정 보드의 visible 값을 true로 설정한다.
   */
  @Transactional
  public void enableVisibility(Board board, Long userId) {
    Optional<BoardMember> optionalBm =
      boardMemberRepository.findByBoard_IdAndUser_Id(board.getId(), userId);

    BoardMember bm = optionalBm.get();
    if (!bm.isVisible()) {
      bm.updateVisible(true);
      boardMemberRepository.save(bm);
    }
  }

  /**
   * 사용자가 해당 보드의 OWNER인지 여부를 확인한다.
   */
  @Transactional(readOnly = true)
  public boolean isOwner(Long boardId, Long userId) {
    return boardMemberRepository.findByBoard_IdAndUser_Id(boardId, userId)
      .map(bm -> bm.getRole() == BoardMemberRole.OWNER)
      .orElse(false);
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
