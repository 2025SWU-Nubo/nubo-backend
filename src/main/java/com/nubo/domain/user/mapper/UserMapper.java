package com.nubo.domain.user.mapper;

import com.nubo.api.auth.dto.GoogleUserInfoDto;
import com.nubo.domain.user.dto.UserInfoDto;
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
      .provider(Provider.GOOGLE)
      .providerUserId(info.getSub())
      .nickname(info.getName())
      .profileImage(info.getPicture())
      .build();
  }

  /**
   * User 엔티티를 공통 응답용 DTO로 변환한다.
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

}
