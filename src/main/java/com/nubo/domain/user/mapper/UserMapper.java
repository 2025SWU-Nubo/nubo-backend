package com.nubo.domain.user.mapper;

import com.nubo.auth.dto.GoogleUserInfoDto;
import com.nubo.domain.user.dto.MyPageResponseDto;
import com.nubo.domain.user.dto.UserInfoDto;
import com.nubo.domain.user.dto.UserSearchResponseDto;
import com.nubo.domain.user.entity.User;
import com.nubo.domain.user.type.Provider;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

  /**
   * Google 사용자 정보를 기반으로 User 엔티티를 생성한다.
   *
   * @param info Google OAuth로부터 받은 사용자 정보
   * @return 생성된 User 엔티티
   */
  public static User fromGoogleUserInfo(GoogleUserInfoDto info) {
    return User.builder()
      .email(info.getEmail())
      .provider(Provider.GOOGLE)
      .providerUserId(info.getSub())
      .nickname(info.getName())
      .profileImage(info.getPicture())
      .build();
  }

  /**
   * User 엔티티를 로그인 응답용 DTO로 변환한다.
   *
   * @param user User 엔티티
   * @return 사용자 정보 DTO
   */
  public static UserInfoDto toDto(User user) {
    return UserInfoDto.builder()
      .id(user.getId())
      .nickname(user.getNickname())
      .profileImage(user.getProfileImage())
      .build();
  }

  /**
   * User 엔티티를 검색 응답 DTO로 변환한다.
   *
   * @param user User 엔티티
   * @return UserSearchResponseDto
   */
  public UserSearchResponseDto toSearchResponseDto(User user) {
    return UserSearchResponseDto.builder()
      .id(user.getId())
      .nickname(user.getNickname())
      .email(user.getEmail())
      .profileImage(user.getProfileImage())
      .build();
  }

  /**
   * User 엔티티를 마이페이지 응답 DTO로 변환한다.
   *
   * @param user User 엔티티
   * @return MyPageResponseDto
   */
  public MyPageResponseDto toMyPageResponseDto(User user) {
    return MyPageResponseDto.builder()
      .name(user.getNickname())
      .email(user.getEmail())
      .profileImage(user.getProfileImage())
      .build();
  }
}
