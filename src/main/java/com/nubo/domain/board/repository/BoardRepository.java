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

  // 특정 사용자의 1차 보드 목록 조회 (섹션 제외, 기본보드 포함)
  @Query("""
      SELECT DISTINCT b
      FROM Board b
      WHERE b.boardType = :boardType
        AND (b.user.id = :userId OR b.user IS NULL)
        AND EXISTS (
          SELECT 1
          FROM BoardCard bc
          JOIN bc.card c
          WHERE bc.board.id = b.id
            AND c.deletedAt IS NULL
        )
    """)
  List<Board> findVisibleBoardsForUser(@Param("userId") Long userId,
    @Param("boardType") BoardType boardType);

  // 보드 하위 섹션 목록 조회
  List<Board> findByParentBoard_Id(Long parentBoardId);

  // 기본 제공 보드 매핑용 (사용자 + 이름 기준)
  Optional<Board> findByUserIdAndName(Long userId, String name);

  // 보드별 섹션/카드 개수 통계 조회
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
