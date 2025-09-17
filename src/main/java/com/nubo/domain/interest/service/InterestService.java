package com.nubo.domain.interest.service;

import com.nubo.domain.board.repository.BoardMemberRepository;
import com.nubo.domain.board.repository.BoardRepository;
import com.nubo.domain.board.type.DefaultBoard;
import com.nubo.domain.interest.dto.InterestSetupRequestDto;
import com.nubo.domain.interest.dto.InterestSetupResponseDto;
import com.nubo.domain.user.entity.User;
import com.nubo.domain.user.repository.UserRepository;
import com.nubo.global.error.ErrorCode;
import com.nubo.global.error.exception.ApiException;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InterestService {

  // 건너뛰기 시 기본으로 노출할 보드 ID (샘플값, Config로 빼도 됨)
  private static final List<Long> DEFAULT_SKIP_BOARD_IDS = List.of(1L, 2L, 3L);

  private final UserRepository userRepository;
  private final BoardMemberRepository boardMemberRepository;
  private final BoardRepository boardRepository;

  /**
   * 관심사 설정을 처리한다.
   *
   * @param userId     로그인된 사용자 ID
   * @param requestDto 선택된 보드 ID, skip 여부
   * @return 관심사 설정 결과 응답 DTO
   */
  @Transactional
  public InterestSetupResponseDto setupInterests(Long userId, InterestSetupRequestDto requestDto) {
    // 1. 유저 조회
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));

    // 2. 이미 완료된 경우 → 200 OK + completed=true
    if (user.isInterestSetupCompleted()) {
      return InterestSetupResponseDto.builder()
        .completed(true)
        .selectedCount(0)
        .build();
    }

    // 3. visible 초기화 (선택된 것만 다시 true 처리)
    boardMemberRepository.bulkResetVisible(userId);

    List<Long> targetBoardIds;

    if (requestDto.isSkip()) {
      // 건너뛰기: 기본 추천 보드 노출
      targetBoardIds = DefaultBoard.getSkipDefaults().stream()
        .map(board -> boardRepository.findByUserIdAndName(userId, board.getDisplayName())
          .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND))
          .getId()
        )
        .toList();
    } else {
      // 직접 선택
      targetBoardIds = requestDto.getSelectedBoardIds();
      if (targetBoardIds == null || targetBoardIds.isEmpty()) {
        throw new ApiException(ErrorCode.FIELD_REQUIRED);
      }
    }

    // 4. 선택된 보드들 visible=true 업데이트
    boardMemberRepository.bulkSetVisibleTrue(targetBoardIds, userId);

    // 5. 유저 상태 업데이트
    user.markInterestSetupCompleted();
    userRepository.save(user);

    // 6. 응답 반환
    return InterestSetupResponseDto.builder()
      .completed(true)
      .selectedCount(targetBoardIds.size())
      .build();
  }
}
