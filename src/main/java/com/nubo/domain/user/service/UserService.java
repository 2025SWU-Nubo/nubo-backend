package com.nubo.domain.user.service;

import com.nubo.domain.board.entity.Board;
import com.nubo.domain.board.repository.BoardRepository;
import com.nubo.domain.board.type.BoardSource;
import com.nubo.domain.board.type.BoardType;
import com.nubo.domain.board.type.DefaultBoard;
import com.nubo.domain.user.dto.MyPageResponseDto;
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
   * 소셜 로그인으로 전달된 사용자 정보로
   * 기존 사용자를 조회하거나, 없으면 새로 생성한다.
   * 신규 생성 시 기본 보드들도 함께 생성된다.
   *
   * @param userCandidate 소셜 로그인으로부터 생성된 User 후보 정보
   * @return 기존 사용자 또는 새로 저장된 사용자
   */
  @Transactional
  public User getOrCreateUser(User userCandidate) {
    return userRepository.findByProviderAndProviderUserId(
      userCandidate.getProvider(),
      userCandidate.getProviderUserId()
    ).orElseGet(() -> {
      // 1. 사용자 저장
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
   * @exception ApiException 사용자가 존재하지 않을 경우 (UNAUTHORIZED_CLIENT)
   */
  public User getUserById(Long id) {
    return userRepository.findById(id)
      .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED_CLIENT));
  }

  /**
   * 주어진 사용자 ID에 해당하는 프로필 정보를 조회한다.
   *
   * @param id 사용자 ID
   * @return 사용자의 이름, 이메일, 프로필 이미지 등을 담은 응답 DTO
   */
  @Transactional(readOnly = true)
  public MyPageResponseDto getMyPage(Long id) {
    User user = getUserById(id);
    return userMapper.toMyPageResponseDto(user);
  }

  /**
   * 이메일 키워드로 사용자 목록을 검색한다.
   * 자기 자신은 결과에서 제외된다.
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

  /**
   * 이메일 리스트로 사용자들을 조회한다.
   * 입력 값은 소문자+trim으로 normalize 처리된다.
   *
   * @param emails 이메일 문자열 리스트
   * @return 조회된 사용자 리스트 (없으면 빈 리스트)
   */
  @Transactional(readOnly = true)
  public List<User> getUsersByEmails(List<String> emails) {
    if (emails == null || emails.isEmpty()) {
      return List.of();
    }
    List<String> norm = emails.stream()
      .filter(Objects::nonNull)
      .map(String::trim)
      .map(String::toLowerCase)
      .filter(s -> !s.isBlank())
      .toList();
    return userRepository.findByEmailIn(norm);
  }

}
