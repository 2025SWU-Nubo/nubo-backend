package com.nubo.domain.card.repository;

import com.nubo.domain.board.entity.Board;
import com.nubo.domain.card.entity.Card;
import com.nubo.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardRepository extends JpaRepository<Card, Long> {

  // =========================
  // 조회
  // =========================

  // 사용자 카드 전체 조회 (최신순)
  List<Card> findAllByUserAndDeletedAtIsNullOrderByCreatedAtDesc(User user);

  // 사용자 카드 전체 조회 (오래된순)
  List<Card> findAllByUserAndDeletedAtIsNullOrderByCreatedAtAsc(User user);

  // 사용자 카드 전체 조회 (가나다순)
  List<Card> findAllByUserAndDeletedAtIsNullOrderByTitleAsc(User user);

  // 사용자 카드 단건 조회 (삭제되지 않은 것만)
  @Query("""
    SELECT DISTINCT c
    FROM Card c
    JOIN BoardCard bc ON bc.card.id = c.id
    JOIN Board b ON bc.board.id = b.id
    WHERE c.id = :cardId
      AND c.deletedAt IS NULL
      AND (
        c.user.id = :userId
        OR EXISTS (
          SELECT 1
          FROM BoardMember bm
          WHERE bm.board.id = b.id
            AND bm.user.id = :userId
        )
      )
    """)
  Optional<Card> findAccessibleById(@Param("cardId") Long cardId, @Param("userId") Long userId);

  // 특정 영상으로 생성된 카드 조회 (동일 영상 중복 방지용)
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      select c
      from Card c join c.video v
      where c.user = :user and v.id = :videoId
    """)
  List<Card> findAnyByUserAndVideoIdForUpdate(@Param("user") User user,
    @Param("videoId") String videoId);

  // 보드에 연결된 카드들 조회 (최신순)
  @Query("""
      select c
        from BoardCard bc
        join bc.card c
       where bc.board.id = :boardId
         and c.deletedAt is null
       order by c.createdAt desc
    """)
  List<Card> findByBoardIdOrderByCreatedAtDesc(@Param("boardId") Long boardId);

  // 보드에 연결된 카드 개수
  @Query("""
      select count(c)
        from BoardCard bc
        join bc.card c
       where bc.board.id = :boardId
         and c.deletedAt is null
    """)
  long countActiveByBoardId(@Param("boardId") Long boardId);

  // 보드에 연결된 최신 카드 N개 (썸네일용)
  @Query("""
      select c
        from BoardCard bc
        join bc.card c
       where bc.board = :board
         and c.deletedAt is null
       order by c.createdAt desc
    """)
  List<Card> findRecentCardsByBoard(@Param("board") Board board, Pageable pageable);

  // 미열람 카드의 썸네일 리스트 랜덤 조회
  @Query(value = """
        SELECT c.*
        FROM board_card bc
        JOIN card c ON c.id = bc.card_id
        WHERE bc.board_id = :boardId
          AND c.deleted_at IS NULL
          AND NOT EXISTS (
            SELECT 1
            FROM card_user_status cus
            WHERE cus.user_id = :userId
              AND cus.card_id = c.id
              AND cus.viewed_at IS NOT NULL
          )
        ORDER BY RAND()
        LIMIT :limit
    """, nativeQuery = true)
  List<Card> findUnviewedCardsByBoard(@Param("userId") Long userId,
    @Param("boardId") Long boardId,
    @Param("limit") int limit);

  // 카드 검색 (제목, 내용, 태그 내에서의 키워드 "일부" 일치)
  @Query("""
    SELECT DISTINCT c
    FROM Card c
    JOIN BoardCard bc ON bc.card.id = c.id
    JOIN Board b ON bc.board.id = b.id
    WHERE c.deletedAt IS NULL
      AND (
        c.user.id = :userId
        OR EXISTS (
          SELECT 1 FROM BoardMember bm
          WHERE bm.board.id = b.id
            AND bm.user.id = :userId
        )
      )
      AND (
        LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(c.summary) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(c.tags) LIKE LOWER(CONCAT('%', :keyword, '%'))
      )
    ORDER BY
      CASE WHEN :sort = 'LATEST' THEN c.createdAt END DESC,
      CASE WHEN :sort = 'OLDEST' THEN c.createdAt END ASC,
      CASE WHEN :sort = 'ALPHABET' THEN c.title END ASC
    """)
  List<Card> searchAccessibleCards(
    @Param("userId") Long userId,
    @Param("keyword") String keyword,
    @Param("sort") String sort
  );

  // =========================
  // 락/동시성 제어
  // =========================

  // 카드 단건 조회 (수정/삭제 시 PESSIMISTIC_WRITE 락)
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from Card c where c.id = :id")
  Optional<Card> findByIdForUpdate(@Param("id") Long id);

  // =========================
  // 소프트 삭제
  // =========================

  // 단건 소프트 삭제
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
    update Card c
       set c.deletedAt = :now,
           c.deletedBy = :userId
     where c.id = :id
       and c.deletedAt is null
    """)
  int softDeleteById(@Param("id") Long id,
    @Param("userId") Long userId,
    @Param("now") Instant now);

  // 다건 소프트 삭제
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
    update Card c
       set c.deletedAt = :now,
           c.deletedBy = :userId
     where c.id in :ids
       and c.deletedAt is null
    """)
  int softDeleteByIds(@Param("ids") Collection<Long> ids,
    @Param("userId") Long userId,
    @Param("now") Instant now);
}
