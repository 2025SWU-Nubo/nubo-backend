package com.nubo.global.auth;

import com.nubo.domain.user.entity.User;
import java.util.Collection;
import java.util.Collections;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class CustomUserDetails implements UserDetails {

  private final Long id;
  private final String provider;
  private final String nickname;
  private final String profileImageUrl;

  public CustomUserDetails(User user) {
    this.id = user.getId();
    this.provider = user.getProvider().name(); // GOOGLE, KAKAO, NAVER
    this.nickname = user.getNickname();
    this.profileImageUrl = user.getProfileImageUrl();
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return Collections.singleton(() -> "ROLE_USER");
  }

  @Override
  public String getPassword() {
    return null; // 소셜 로그인이라 패스워드 없음
  }

  @Override
  public String getUsername() {
    return id.toString(); // 유니크한 식별자 (로그인엔 사용되지 않음)
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}
