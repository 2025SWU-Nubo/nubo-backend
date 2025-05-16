package com.nubo.domain.user.repository;

import com.nubo.domain.user.entity.User;
import com.nubo.domain.user.type.Provider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  // 소셜 로그인 정보로 유저 조회
  Optional<User> findByProviderAndProviderUserId(Provider provider, String providerUserId);
}
