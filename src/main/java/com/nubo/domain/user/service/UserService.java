package com.nubo.domain.user.service;

import com.nubo.domain.board.entity.Board;
import com.nubo.domain.board.repository.BoardRepository;
import com.nubo.domain.board.type.BoardSource;
import com.nubo.domain.board.type.BoardType;
import com.nubo.domain.board.type.DefaultBoard;
import com.nubo.domain.user.dto.UserSearchResponseDto;
import com.nubo.domain.user.entity.User;
import com.nubo.domain.user.mapper.UserMapper;
import com.nubo.domain.user.repository.UserRepository;
import com.nubo.global.auth.UserUtil;
import com.nubo.global.error.ErrorCode;
import com.nubo.global.error.exception.ApiException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final BoardRepository boardRepository;

  private final UserMapper userMapper;
  private final UserUtil userUtil;

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

      // 2. 기본 보드 생성
      List<Board> defaults = Arrays.stream(DefaultBoard.values())
        .map(defaultBoard -> Board.builder()
          .name(defaultBoard.getDisplayName())
          .boardType(BoardType.BOARD)
          .source(BoardSource.AI)
          .user(newUser)
          .build())
        .toList();

      boardRepository.saveAll(defaults);

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

  /**
   * 이메일 키워드를 기반으로 사용자 목록을 검색한다.
   * <p>이메일 주소에 부분 일치하는 사용자를 반환한다.</p>
   *
   * @param keyword 검색할 이메일 키워드 (대소문자 구분 없음)
   * @return UserSearchResponseDto 리스트
   */
  @Transactional(readOnly = true)
  public List<UserSearchResponseDto> searchUsersByEmail(String keyword) {
    Long currentUserId = userUtil.getAuthenticatedUserId();

    List<User> users = userRepository.findByEmailContainingIgnoreCase(keyword);
    return users.stream()
      .filter(user -> !user.getId().equals(currentUserId)) // 자기 자신 제외
      .map(userMapper::toSearchResponseDto)
      .toList();
  }

  @Transactional(readOnly = true)
  public List<User> getUsersByEmails(List<String> emails) {
    if (emails == null || emails.isEmpty()) {
      return List.of();
    }
    // 이메일 normalize (trim+lower) 일관성 유지
    List<String> norm = emails.stream()
      .filter(Objects::nonNull)
      .map(String::trim)
      .map(String::toLowerCase)
      .filter(s -> !s.isBlank())
      .toList();
    return userRepository.findByEmailIn(norm);
  }

}
