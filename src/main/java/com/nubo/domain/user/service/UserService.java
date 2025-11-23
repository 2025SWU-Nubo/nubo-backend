package com.nubo.domain.user.service;

import com.nubo.domain.board.entity.Board;
import com.nubo.domain.board.entity.BoardMember;
import com.nubo.domain.board.mapper.BoardMapper;
import com.nubo.domain.board.mapper.BoardMemberMapper;
import com.nubo.domain.board.repository.BoardMemberRepository;
import com.nubo.domain.board.repository.BoardRepository;
import com.nubo.domain.user.dto.MyPageResponseDto;
import com.nubo.domain.user.dto.UserProfileUpdateResponseDto;
import com.nubo.domain.user.dto.UserPushSettingRequestDto;
import com.nubo.domain.user.dto.UserSearchResponseDto;
import com.nubo.domain.user.dto.UserWithStatusDto;
import com.nubo.domain.user.entity.User;
import com.nubo.domain.user.mapper.UserMapper;
import com.nubo.domain.user.repository.UserRepository;
import com.nubo.global.auth.UserUtil;
import com.nubo.global.error.ErrorCode;
import com.nubo.global.error.exception.ApiException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final UserUtil userUtil;

  private final BoardRepository boardRepository;
  private final BoardMemberRepository boardMemberRepository;
  private final BoardMemberMapper boardMemberMapper;
  private final BoardMapper boardMapper;

  /**
   * 소셜 로그인으로 전달된 사용자 정보로
   * 기존 사용자를 조회하거나, 없으면 새로 생성한다.
   * 신규 생성 시 기본 보드들도 함께 생성된다.
   *
   * @param userCandidate 소셜 로그인으로부터 생성된 User 후보 정보
   * @return 기존 사용자 또는 새로 저장된 사용자
   */
  @Transactional
  public UserWithStatusDto getOrCreateUser(User userCandidate) {
    boolean reactivated = false;
    boolean isNewUser = false;
    User user;

    // 기존 사용자 조회
    Optional<User> existingOpt = userRepository.findByProviderAndProviderUserId(
      userCandidate.getProvider(),
      userCandidate.getProviderUserId()
    );

    if (existingOpt.isPresent()) {
      // ✅ 기존 유저 존재
      user = existingOpt.get();

      // 탈퇴 이력이 있으면 복구 처리
      if (user.getDeletedAt() != null) {
        user.setDeletedAt(null);
        userRepository.save(user);
        reactivated = true; // 복구됨
      }

    } else {
      // ✅ 신규 유저 생성
      user = userRepository.save(userCandidate);
      isNewUser = true;

      // 기본 보드 생성
      List<Board> defaultBoards = boardMapper.toDefaultBoards(user);
      List<BoardMember> memberships = boardMemberMapper.toDefaultBoardMembers(defaultBoards, user);
      boardRepository.saveAll(defaultBoards);
      boardMemberRepository.saveAll(memberships);

      // 푸시알림 기본값 true 설정
      user.setRemindEnabled(true);
      user.setPushEnabled(true);
    }

    return new UserWithStatusDto(user, reactivated, isNewUser);
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

    if (keyword == null || keyword.isBlank()) {
      throw new ApiException(ErrorCode.FIELD_REQUIRED);
    }

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

  /**
   * 리마인더를 위한 사용자들을 조회한다.
   *
   * @return 조회된 사용자의 id 리스트
   */
  @Transactional(readOnly = true)
  public List<Long> getUserIdsForReminder() {
    return userRepository.findUserIdsForReminder();
  }

  /**
   * 주어진 사용자 ID의 닉네임을 수정한다.
   *
   * @param userId   사용자 ID
   * @param nickname 새 닉네임
   */
  @Transactional
  public void updateNickname(Long userId, String nickname) {
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));
    user.updateNickname(nickname);
  }

  /**
   * 주어진 사용자 ID의 프로필 이미지를 수정한다.
   *
   * @param userId   사용자 ID
   * @param imageUrl 새 프로필 이미지 url
   * @return 업데이트된 정보를 담은 DTO
   */
  @Transactional
  public UserProfileUpdateResponseDto updateProfileImage(Long userId, String imageUrl) {
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));

    user.updateProfileImageUrl(imageUrl);

    return UserProfileUpdateResponseDto.builder()
      .id(user.getId())
      .email(user.getEmail())
      .nickname(user.getNickname())
      .profileImageUrl(user.getProfileImageUrl())
      .build();
  }

  /**
   * 주어진 사용자 ID의 푸시알림 설정 여부를 수정한다.
   *
   * @param userId 사용자 ID
   * @param dto    푸시알림 설정 정보를 담은 DTO
   */
  @Transactional
  public void updatePushSettings(Long userId, UserPushSettingRequestDto dto) {
    User user = getUserById(userId);

    // pushEnabled 값이 들어왔을 때
    if (dto.getPushEnabled() != null) {
      user.setPushEnabled(dto.getPushEnabled());
      // 전체 푸시를 끄면 리마인더도 자동으로 끔
      if (!dto.getPushEnabled()) {
        user.setRemindEnabled(false);
      }
    }

    // remindEnabled 값이 들어왔을 때
    if (dto.getRemindEnabled() != null) {
      // 단, 전체 푸시가 켜져 있을 때만 반영
      if (user.isPushEnabled()) {
        user.setRemindEnabled(dto.getRemindEnabled());
      } else {
        // 전체 푸시가 꺼져 있으면 리마인더는 무조건 false
        user.setRemindEnabled(false);
      }
    }
  }

  /**
   * 회원 탈퇴 (Soft Delete)
   *
   * @param userId 탈퇴할 사용자 ID
   * @exception ApiException 존재하지 않거나 이미 탈퇴된 사용자인 경우
   */
  @Transactional
  public void deactivateUser(Long userId) {
    User user = userRepository.findActiveById(userId)
      .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));

    // 이미 탈퇴된 유저인지 한 번 더 체크
    if (user.getDeletedAt() != null) {
      throw new ApiException(ErrorCode.ALREADY_DELETED);
    }

    user.markAsDeleted();
    userRepository.save(user);
  }

  /**
   * 현재 로그인한 유저를 탈퇴 처리 한다.
   */
  @Transactional
  public void deactivateCurrentUser() {
    Long userId = userUtil.getAuthenticatedUserId();
    deactivateUser(userId);
  }

  /**
   * 모든 활성 사용자 ID 목록 조회
   */
  @Transactional(readOnly = true)
  public List<Long> getAllActiveUserIds() {
    return userRepository.findAllActiveUserIds();
  }
}
