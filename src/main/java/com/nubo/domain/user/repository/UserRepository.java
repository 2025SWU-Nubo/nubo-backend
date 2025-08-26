package com.nubo.domain.user.repository;

import com.nubo.domain.user.entity.User;
import com.nubo.domain.user.type.Provider;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  // 소셜 로그인 Provider + ProviderUserId 로 사용자 조회
  Optional<User> findByProviderAndProviderUserId(Provider provider, String providerUserId);

  // 이메일 부분 검색 (공유 보드 초대용)
  List<User> findByEmailContainingIgnoreCase(String email);

  // 여러 이메일로 사용자 조회
  List<User> findByEmailIn(Collection<String> emails);
}
