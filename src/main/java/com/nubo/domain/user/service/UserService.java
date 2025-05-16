package com.nubo.domain.user.service;

import com.nubo.domain.user.entity.User;
import com.nubo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

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
    ).orElseGet(() -> userRepository.save(userCandidate));
  }

}
