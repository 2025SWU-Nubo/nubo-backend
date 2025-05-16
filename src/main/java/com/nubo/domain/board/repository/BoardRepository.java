package com.nubo.domain.board.repository;

import com.nubo.domain.board.entity.Board;
import com.nubo.domain.board.type.BoardType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardRepository extends JpaRepository<Board, Long> {

  // 특정 사용자의 특정 보드(섹션)
  Optional<Board> findByIdAndUserId(Long id, Long userId);

  // 특정 사용자의 1차 보드만 (섹션 제외)
  List<Board> findByUserIdAndBoardType(Long userId, BoardType boardType);

  // 보드 하위 섹션 리스트
  List<Board> findByParentBoard(Board parentBoard);
}
