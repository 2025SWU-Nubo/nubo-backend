package com.nubo.domain.user.controller;

import com.nubo.domain.user.dto.MyPageResponseDto;
import com.nubo.domain.user.dto.UserSearchResponseDto;
import com.nubo.domain.user.service.UserService;
import com.nubo.global.auth.UserUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

  private final UserService userService;
  private final UserUtil userUtil;

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
}
