package com.nubo.domain.user.service;

import com.nubo.domain.board.entity.Board;
import com.nubo.domain.board.repository.BoardRepository;
import com.nubo.domain.board.type.BoardSource;
import com.nubo.domain.board.type.BoardType;
import com.nubo.domain.board.type.DefaultBoard;
import com.nubo.domain.user.entity.User;
import com.nubo.domain.user.repository.UserRepository;
import com.nubo.global.error.ErrorCode;
import com.nubo.global.error.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final BoardRepository boardRepository;

  /**
   * 주어진 사용자 정보로 기존 사용자를 조회하거나, 없으면 새로 생성한다.
   *
   * @param userCandidate Google 로그인으로부터 생성된 User 후보 정보
   * @return 기존 사용자 또는 새로 저장된 사용자
   */
  @Transactional
  public User getOrCreateUser(User userCandidate) {
    return userRepository.findByProviderAndProviderUserId(
      userCandidate.getProvider(),
      userCandidate.getProviderUserId()
    ).orElseGet(() -> {
      // 1. 유저 저장
      User newUser = userRepository.save(userCandidate);

      // 2. 기본 보드 10개 생성
      for (DefaultBoard defaultBoard : DefaultBoard.values()) {
        Board board = Board.builder()
          .name(defaultBoard.getDisplayName())
          .boardType(BoardType.BOARD)
          .source(BoardSource.AI)
          .user(newUser)
          .build();

        boardRepository.save(board);
      }

      return newUser;
    });
  }

  /**
   * 주어진 ID로 사용자를 조회한다.
   * 사용자가 존재하지 않으면 인증 예외를 발생시킨다.
   *
   * @param id 사용자 ID
   * @return 조회된 사용자 엔티티
   * @exception ApiException 사용자가 존재하지 않는 경우 UNAUTHORIZED_CLIENT 예외 발생
   */
  public User getUserById(Long id) {
    return userRepository.findById(id)
      .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED_CLIENT));
  }

}
