package com.nubo.domain.user.repository;

import com.nubo.domain.board.type.DefaultBoard;
import com.nubo.domain.user.entity.UserInterest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserInterestRepository extends JpaRepository<UserInterest, Long> {

  List<UserInterest> findAllByUserId(Long userId);

  void deleteByUserId(Long userId);

  boolean existsByUserIdAndCategory(Long userId, DefaultBoard category);
}