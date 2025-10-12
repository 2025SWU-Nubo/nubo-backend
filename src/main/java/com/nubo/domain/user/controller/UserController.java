package com.nubo.domain.user.controller;

import com.nubo.domain.user.dto.MyPageResponseDto;
import com.nubo.domain.user.dto.PresignedUrlResponseDto;
import com.nubo.domain.user.dto.UserNicknameUpdateRequestDto;
import com.nubo.domain.user.dto.UserProfileImageUpdateRequestDto;
import com.nubo.domain.user.dto.UserProfileUpdateResponseDto;
import com.nubo.domain.user.dto.UserPushSettingRequestDto;
import com.nubo.domain.user.dto.UserSearchResponseDto;
import com.nubo.domain.user.service.UserService;
import com.nubo.global.auth.UserUtil;
import com.nubo.global.s3.S3Service;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

  private final UserService userService;
  private final UserUtil userUtil;
  private final S3Service s3Service;

  /**
   * 이메일로 사용자를 검색하는 API
   * 입력한 이메일 키워드에 부분 일치하는 사용자 목록을 조회한다.
   *
   * @param email 검색할 이메일 키워드
   * @return 검색된 사용자 리스트 (users 키로 감싼 JSON 응답)
   */
  @GetMapping("/search")
  public ResponseEntity<List<UserSearchResponseDto>> searchUsers(@RequestParam String email) {
    List<UserSearchResponseDto> users = userService.searchUsersByEmail(email);
    return ResponseEntity.ok(users);
  }

  /**
   * 현재 로그인한 사용자의 마이페이지 정보를 조회한다.
   *
   * @return 사용자의 이름, 이메일, 프로필 이미지 등을 담은 응답 DTO
   */
  @GetMapping("/mypage")
  public ResponseEntity<MyPageResponseDto> getMyPage() {
    Long userId = userUtil.getAuthenticatedUserId();
    MyPageResponseDto dto = userService.getMyPage(userId);
    return ResponseEntity.ok(dto);
  }

  /**
   * 현재 로그인한 사용자의 닉네임을 수정한다.
   *
   * @param dto 수정할 닉네임이 담긴 요청 DTO
   * @return 204 No Content
   */
  @PatchMapping("/me/nickname")
  public ResponseEntity<Void> updateNickname(@RequestBody @Valid UserNicknameUpdateRequestDto dto) {
    Long userId = userUtil.getAuthenticatedUserId();
    userService.updateNickname(userId, dto.getNickname());
    return ResponseEntity.noContent().build();
  }

  /**
   * 프로필 이미지 업로드를 위한 presigned URL을 발급한다.
   *
   * @param fileName 업로드할 파일명
   * @return 200 OK + presigned URL 응답
   */
  @GetMapping("/profile-image/presigned-url")
  public ResponseEntity<PresignedUrlResponseDto> getProfileImagePresignedUrl(
    @RequestParam String fileName) {
    String url = s3Service.generateUploadUrl("profile", fileName);
    return ResponseEntity.ok(new PresignedUrlResponseDto(url));
  }

  /**
   * 현재 로그인한 사용자의 프로필 이미지 URL을 수정한다.
   *
   * @param dto 수정할 프로필 이미지 URL이 담긴 요청 DTO
   * @return 200 OK + 수정된 프로필 정보 응답
   */
  @PatchMapping("/me/profile-image")
  public ResponseEntity<UserProfileUpdateResponseDto> updateProfileImage(
    @RequestBody UserProfileImageUpdateRequestDto dto
  ) {
    Long userId = userUtil.getAuthenticatedUserId();
    UserProfileUpdateResponseDto response =
      userService.updateProfileImage(userId, dto.getProfileImageUrl());
    return ResponseEntity.ok(response);
  }

  /**
   * 현재 로그인한 사용자의 푸시알림 설정을 업데이트 한다.
   *
   * @param dto 푸시알림 설정 여부(t/f)가 담긴 요청 DTO
   * @return 204 No Content
   */
  @PatchMapping("/me/notification")
  public ResponseEntity<Void> updatePushSettings(
    @RequestBody UserPushSettingRequestDto dto
  ) {
    Long userId = userUtil.getAuthenticatedUserId();
    userService.updatePushSettings(userId, dto);
    return ResponseEntity.noContent().build();
  }

  /**
   * 현재 로그인한 사용자를 탈퇴 처리한다.
   *
   * @return 204 No Content
   */
  @DeleteMapping("/me")
  public ResponseEntity<Void> deleteCurrentUser() {
    userService.deactivateCurrentUser();
    return ResponseEntity.noContent().build();
  }
}
