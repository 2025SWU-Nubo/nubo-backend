package com.nubo.domain.board.repository;

import com.nubo.domain.board.dto.BoardStatsDto;
import com.nubo.domain.board.entity.Board;
import com.nubo.domain.board.type.BoardType;
import com.nubo.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardRepository extends JpaRepository<Board, Long> {

  // 특정 사용자의 1차 보드 목록 조회 (하위에 섹션이나 카드를 보유한 경우) - list
  @Query("""
    SELECT DISTINCT b
    FROM Board b
    WHERE b.boardType = :boardType
      AND b.deletedAt IS NULL
      AND (b.user.id = :userId OR b.user IS NULL)
      AND (
        b.source = 'USER'
        OR EXISTS (
          SELECT 1 FROM BoardCard bc
          WHERE bc.board.id = b.id
            AND bc.card.deletedAt IS NULL
        )
        OR EXISTS (
          SELECT 1 FROM Board s
          WHERE s.parentBoard.id = b.id
            AND s.deletedAt IS NULL
        )
        OR EXISTS (
          SELECT 1 FROM BoardMember bm
          WHERE bm.board.id = b.id
            AND bm.user.id = :userId
            AND bm.visible = true
        )
      )
    ORDER BY
      CASE WHEN :sort = 'LATEST' THEN b.createdAt END DESC,
      CASE WHEN :sort = 'OLDEST' THEN b.createdAt END ASC,
      CASE WHEN :sort = 'ALPHABET' THEN b.name END ASC
    """)
  List<Board> findVisibleBoardsForUser(
    @Param("userId") Long userId,
    @Param("boardType") BoardType boardType,
    @Param("sort") String sort
  );

  // 특정 사용자의 1차 보드 목록 조회 (하위에 섹션이나 카드를 보유한 경우) - paging
  @Query("""
      SELECT DISTINCT b
      FROM Board b
      WHERE b.boardType = :boardType
        AND b.deletedAt IS NULL
        AND (b.user.id = :userId OR b.user IS NULL)
        AND (
          b.source = 'USER'
          OR EXISTS (
            SELECT 1 FROM BoardCard bc
            WHERE bc.board.id = b.id
              AND bc.card.deletedAt IS NULL
          )
          OR EXISTS (
            SELECT 1 FROM Board s
            WHERE s.parentBoard.id = b.id
              AND s.deletedAt IS NULL
          )
          OR EXISTS (
            SELECT 1 FROM BoardMember bm
            WHERE bm.board.id = b.id
              AND bm.user.id = :userId
              AND bm.visible = true
          )
        )
    """)
  Page<Board> findVisibleBoardsForUser(
    @Param("userId") Long userId,
    @Param("boardType") BoardType boardType,
    Pageable pageable
  );

  // 즐겨찾기된 보드
  @Query("""
      SELECT bm.board
      FROM BoardMember bm
      WHERE bm.user.id = :userId
        AND bm.favorite = true
        AND bm.board.boardType = 'BOARD'
        AND bm.board.deletedAt IS NULL
    """)
  Page<Board> findFavoriteBoards(Long userId, Pageable pageable);

  // 공유 보드
  @Query("""
      SELECT bm.board
      FROM BoardMember bm
      WHERE bm.user.id = :userId
        AND bm.board.shared = true
        AND bm.board.boardType = 'BOARD'
        AND bm.board.deletedAt IS NULL
    """)
  Page<Board> findSharedBoards(Long userId, Pageable pageable);

  // 즐겨찾기된 섹션 조회
  @Query("""
      SELECT bm.board
      FROM BoardMember bm
      WHERE bm.user.id = :userId
        AND bm.favorite = true
        AND bm.board.parentBoard.id = :parentBoardId
        AND bm.board.boardType = 'SECTION'
        AND bm.board.deletedAt IS NULL
    """)
  List<Board> findFavoriteSectionsByParentBoardId(
    @Param("parentBoardId") Long parentBoardId,
    @Param("userId") Long userId
  );

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
      AND b.deletedAt IS NULL
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
    LEFT JOIN FETCH b.sections s
    WHERE b.deletedAt IS NULL
      AND (
          b.user.id = :userId OR b.user IS NULL
          OR EXISTS (
            SELECT 1
            FROM BoardMember bm
            WHERE bm.board.id = b.id
              AND bm.user.id = :userId
          )
      )
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
            FROM Board sb
            WHERE sb.parentBoard.id = b.id
              AND sb.deletedAt IS NULL
          )
          OR EXISTS (
            SELECT 1
            FROM BoardMember bm2
            WHERE bm2.board.id = b.id
              AND bm2.user.id = :userId
              AND bm2.visible = true
          )
      )
      AND b.parentBoard IS NULL
    ORDER BY b.id
    """)
  List<Board> findAllAccessibleBoards(@Param("userId") Long userId);

  // 동일한 이름의 보드가 존재하는지 조회한다.
  boolean existsByUser_IdAndNameIgnoreCase(Long userId, String name);

  // 사용자의 기본 보드를 모두 조회한다.
  @Query("""
    SELECT b
    FROM Board b
    WHERE b.user.id = :userId
      AND b.source = 'AI'
      AND b.boardType = 'BOARD'
      AND b.deletedAt IS NULL
    """)
  List<Board> findAllDefaultBoardsByUserId(@Param("userId") Long userId);

  /**
   * 사용자가 접근 가능한 보드/섹션 중,
   * 하위에 카드 또는 섹션이 존재하는 보드들을 이름으로 검색한다.
   */
  @Query("""
    SELECT DISTINCT b
    FROM Board b
    WHERE b.deletedAt IS NULL
      AND (b.user.id = :userId OR b.user IS NULL
             OR EXISTS (
               SELECT 1 FROM BoardMember bm
               WHERE bm.board.id = b.id
                 AND bm.user.id = :userId
             )
      )
      AND (
        b.source = 'USER'
        OR (
          b.source = 'AI'
          AND (
            EXISTS (
              SELECT 1 FROM BoardCard bc
              WHERE bc.board.id = b.id
                AND bc.card.deletedAt IS NULL
            )
            OR EXISTS (
              SELECT 1 FROM Board s
              WHERE s.parentBoard.id = b.id
               AND s.deletedAt IS NULL 
            )
          )
        )
      )
      AND LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
    ORDER BY
      CASE WHEN :sort = 'LATEST' THEN b.createdAt END DESC,
      CASE WHEN :sort = 'OLDEST' THEN b.createdAt END ASC,
      CASE WHEN :sort = 'ALPHABET' THEN b.name END ASC
    """)
  List<Board> searchBoardsByName(
    @Param("userId") Long userId,
    @Param("keyword") String keyword,
    @Param("sort") String sort
  );

  // 이름 중복 확인
  @Query("""
    SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
    FROM Board b
    WHERE b.name = :name
      AND b.deletedAt IS NULL
      AND (b.user = :user OR b.source = com.nubo.domain.board.type.BoardSource.AI)
      AND (
        (b.boardType = com.nubo.domain.board.type.BoardType.BOARD AND :parentBoard IS NULL)
        OR (b.boardType = com.nubo.domain.board.type.BoardType.SECTION AND b.parentBoard = :parentBoard)
      )
    """)
  boolean existsByNameConflict(
    @Param("name") String name,
    @Param("user") User user,
    @Param("parentBoard") Board parentBoard
  );

  boolean existsByUserAndNameAndParentBoardIsNull(User user, String candidate);

  // 보드 삭제 (soft-delete)
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
    update Board b
       set b.deletedAt = :now,
           b.deletedBy = :userId
     where b.id in :ids
       and b.deletedAt is null
    """)
  int softDeleteByIds(@Param("ids") List<Long> ids,
    @Param("userId") Long userId,
    @Param("now") LocalDateTime now);

  // 삭제 복원
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
    update Board b
       set b.deletedAt = null,
           b.deletedBy = null
     where b.id in :ids
       and b.deletedAt is not null
    """)
  int restoreByIds(@Param("ids") List<Long> ids);
}
