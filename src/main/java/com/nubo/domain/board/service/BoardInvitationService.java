package com.nubo.domain.board.service;

import com.nubo.domain.board.entity.Board;
import com.nubo.domain.board.entity.BoardInvitation;
import com.nubo.domain.board.entity.BoardMember;
import com.nubo.domain.board.repository.BoardInvitationRepository;
import com.nubo.domain.board.repository.BoardMemberRepository;
import com.nubo.domain.board.type.BoardMemberRole;
import com.nubo.domain.board.type.InvitationStatus;
import com.nubo.domain.notification.service.FcmService;
import com.nubo.domain.notification.service.NotificationService;
import com.nubo.domain.user.entity.User;
import com.nubo.global.error.ErrorCode;
import com.nubo.global.error.exception.ApiException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BoardInvitationService {

  private final BoardInvitationRepository invitationRepository;
  private final BoardMemberRepository boardMemberRepository;
  private final FcmService fcmService;
  private final NotificationService notificationService;

  /**
   * 초대 ID로 초대 조회
   *
   * @param invitationId 초대 ID
   * @return 초대 Optional
   */
  @Transactional(readOnly = true)
  public Optional<BoardInvitation> findById(Long invitationId) {
    return invitationRepository.findById(invitationId);
  }

  /**
   * 초대 삭제 (취소 처리)
   *
   * @param invitation 삭제할 초대 엔티티
   */
  @Transactional
  public void delete(BoardInvitation invitation) {
    invitationRepository.delete(invitation);
  }

  /**
   * 보드에 속한 초대 목록 조회
   *
   * @param board 대상 보드
   * @return 초대 목록
   */
  @Transactional(readOnly = true)
  public List<BoardInvitation> getInvitations(Board board) {
    return invitationRepository.findAllByBoardId(board.getId());
  }

  /**
   * 단일 사용자 초대 생성
   *
   * @param board   초대가 속한 보드
   * @param inviter 초대 발신자 (보드 소유자)
   * @param invitee 초대 대상자
   * @return 생성된 초대
   */
  @Transactional
  public BoardInvitation createInvitation(Board board, User inviter, User invitee) {
    // 이미 멤버인지 확인
    boolean isMember = boardMemberRepository.existsByBoard_IdAndUser_Id(board.getId(),
      invitee.getId());
    if (isMember) {
      return null; // 이미 멤버면 초대 안 함
    }

    // 이미 PENDING 초대가 있는지 확인
    boolean alreadyInvited = invitationRepository
      .findByBoardIdAndInviteeIdAndStatus(board.getId(), invitee.getId(), InvitationStatus.PENDING)
      .isPresent();

    if (alreadyInvited) {
      return null; // 이미 초대 상태라면 무시
    }

    // 새 초대 생성
    BoardInvitation invitation = BoardInvitation.builder()
      .board(board)
      .inviter(inviter)
      .invitee(invitee)
      .status(InvitationStatus.PENDING)
      .build();

    BoardInvitation saved = invitationRepository.save(invitation);

    // 초대 알림 전송
    fcmService.sendBoardInviteNotification(
      invitee.getId(),
      board.getName(),
      inviter.getNickname(),
      board,
      saved
    );

    return saved;
  }

  /**
   * 여러 사용자 초대 생성
   *
   * @param board    초대가 속한 보드
   * @param inviter  초대 발신자
   * @param invitees 초대 대상자 목록
   */
  @Transactional
  public void createInvitations(Board board, User inviter, List<User> invitees) {
    for (User invitee : invitees) {
      createInvitation(board, inviter, invitee);
    }
  }


  /**
   * 초대 수락 처리 (멤버로 편입)
   *
   * @param userId       수락하는 사용자 ID
   * @param invitationId 초대 ID
   */
  @Transactional
  public void acceptInvitation(Long userId, Long invitationId) {
    BoardInvitation invitation = invitationRepository
      .findByIdAndInviteeId(invitationId, userId)
      .orElseThrow(() -> new ApiException(ErrorCode.ACCESS_DENIED));

    Board board = invitation.getBoard();
    board.getName();
    board.getId();

    if (invitation.getStatus() != InvitationStatus.PENDING) {
      throw new ApiException(ErrorCode.INVALID_STATE);
    }

    invitation.accept();

    // 멤버 추가
    boardMemberRepository.save(BoardMember.builder()
      .board(board)
      .user(invitation.getInvitee())
      .role(BoardMemberRole.ADMIN) // 정책상 ADMIN
      .visible(true)
      .build());

    // 초대 알림 숨기기
    notificationService.hideByInvitationId(invitation.getId());

    // 초대한 사람에게 알림
    fcmService.sendBoardAcceptNotification(
      invitation.getInviter().getId(),
      invitation.getInvitee().getNickname(),
      board
    );

    // 수락한 본인에게도 알림
    fcmService.sendBoardAddedNotification(
      invitation.getInvitee().getId(),
      board.getName(),
      board
    );
  }

  /**
   * 초대 거절 처리
   *
   * @param userId       거절하는 사용자 ID
   * @param invitationId 초대 ID
   */
  @Transactional
  public void rejectInvitation(Long userId, Long invitationId) {
    BoardInvitation invitation = invitationRepository
      .findByIdAndInviteeId(invitationId, userId)
      .orElseThrow(() -> new ApiException(ErrorCode.ACCESS_DENIED));

    if (invitation.getStatus() != InvitationStatus.PENDING) {
      throw new ApiException(ErrorCode.INVALID_STATE);
    }

    invitation.reject();
  }

}
