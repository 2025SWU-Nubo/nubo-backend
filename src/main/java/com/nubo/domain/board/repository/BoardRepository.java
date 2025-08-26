package com.nubo.domain.board.repository;

import com.nubo.domain.board.dto.BoardStatsDto;
import com.nubo.domain.board.entity.Board;
import com.nubo.domain.board.type.BoardType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardRepository extends JpaRepository<Board, Long> {

  // 특정 사용자의 1차 보드만 (섹션 제외)
  @Query("SELECT b FROM Board b WHERE b.boardType = :boardType AND (b.user.id = :userId OR b.user"
    + " IS NULL)")
  List<Board> findVisibleBoardsForUser(@Param("userId") Long userId,
    @Param("boardType") BoardType boardType);

  // 보드 하위 섹션 리스트
  List<Board> findByParentBoard_Id(Long parentBoardId);

  // 카드 생성 시 기본제공 보드 매핑용
  Optional<Board> findByUserIdAndName(Long userId, String name);

  // 보드 내 섹션, 카드 갯수 조회
  @Query("""
    SELECT new com.nubo.domain.board.dto.BoardStatsDto(
      b.id,
      COUNT(DISTINCT s),
      COUNT(DISTINCT c)
    )
    FROM Board b
    LEFT JOIN Board s ON s.parentBoard.id = b.id
    LEFT JOIN BoardCard bc ON bc.board.id = b.id
    LEFT JOIN Card c ON c.id = bc.card.id
    WHERE b.id IN :boardIds
    GROUP BY b.id
    """)
  List<BoardStatsDto> getBoardStats(@Param("boardIds") List<Long> boardIds);
}
