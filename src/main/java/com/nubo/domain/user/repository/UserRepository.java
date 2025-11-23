package com.nubo.domain.user.repository;

import com.nubo.domain.user.entity.User;
import com.nubo.domain.user.type.Provider;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {

  // 활성 사용자만 포함
  @Query("""
    SELECT u
    FROM User u
    WHERE u.id = :id AND u.deletedAt IS NULL
    """)
  Optional<User> findActiveById(Long id);

  // 소셜 로그인 Provider + ProviderUserId 로 사용자 조회
  Optional<User> findByProviderAndProviderUserId(Provider provider, String providerUserId);

  // 이메일 부분 검색 (공유 보드 초대용)
  @Query("""
    SELECT u
    FROM User u
    WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))
      AND u.deletedAt IS NULL
    """)
  List<User> findByEmailContainingIgnoreCase(String email);

  // 여러 이메일로 사용자 조회
  @Query("""
    SELECT u
    FROM User u
    WHERE u.email IN :emails
      AND u.deletedAt IS NULL
    """)
  List<User> findByEmailIn(Collection<String> emails);

  // 미시청 카드가 1개 이상 존재하는 사용자 리스트 조회
  @Query("""
    SELECT DISTINCT u.id
    FROM User u
    JOIN Card c ON c.user.id = u.id
    LEFT JOIN CardUserStatus cus 
        ON cus.card.id = c.id AND cus.user.id = u.id
    WHERE u.remindEnabled = true
      AND u.deletedAt IS NULL
      AND (cus.viewedAt IS NULL)
    """)
  List<Long> findUserIdsForReminder();

  // 활성 사용자 id 리스트 조회
  @Query("SELECT u.id FROM User u WHERE u.deletedAt IS NULL")
  List<Long> findAllActiveUserIds();
}
