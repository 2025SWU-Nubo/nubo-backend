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

  // 특정 사용자의 1차 보드 목록 조회 (하위에 섹션이나 카드를 보유한 경우)
  @Query("""
      SELECT DISTINCT b
      FROM Board b
      WHERE b.boardType = :boardType
        AND (b.user.id = :userId OR b.user IS NULL)
        AND (
          b.source = 'USER'
          OR EXISTS (
            SELECT 1
            FROM BoardCard bc
            WHERE bc.board.id = b.id
              AND bc.card.deletedAt IS NULL
          )
          OR EXISTS (
            SELECT 1
            FROM Board s
            WHERE s.parentBoard.id = b.id
          )
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

  /**
   * 사용자가 접근 가능한 보드 중에서,
   * 하위에 카드 또는 섹션이 존재하는 보드와 그 섹션들을 함께 조회한다.
   */
  @Query("""
    SELECT DISTINCT b
    FROM Board b
    LEFT JOIN FETCH Board s ON s.parentBoard.id = b.id
    WHERE (
        b.user.id = :userId OR b.user IS NULL
        OR EXISTS (
          SELECT 1
          FROM BoardMember bm
          WHERE bm.board.id = b.id
            AND bm.user.id = :userId
        )
    )
    AND (
        EXISTS (
          SELECT 1
          FROM BoardCard bc
          WHERE bc.board.id = b.id
            AND bc.card.deletedAt IS NULL
        )
        OR EXISTS (
          SELECT 1
          FROM Board sb
          WHERE sb.parentBoard.id = b.id
        )
    )
    ORDER BY b.id
    """)
  List<Board> findAllAccessibleBoardsWithSections(@Param("userId") Long userId);

  // 동일한 이름의 보드가 존재하는지 조회한다.
  boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

}
