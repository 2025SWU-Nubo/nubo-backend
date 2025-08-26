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

  @Query("select c from Card c where c.user = :user and c.deletedAt is null order by c.createdAt "
    + "desc")
  List<Card> findAllActiveByUserOrderByCreatedAtDesc(@Param("user") User user);

  @Query("select c from Card c where c.user = :user and c.deletedAt is null order by c.title asc")
  List<Card> findAllActiveByUserOrderByTitleAsc(@Param("user") User user);

  @Query("select c from Card c where c.id = :cardId and c.user = :user and c.deletedAt is null")
  Optional<Card> findActiveByIdAndUser(@Param("cardId") Long cardId, @Param("user") User user);

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      select c
      from Card c join c.video v
      where c.user = :user and v.id = :videoId
    """)
  List<Card> findAnyByUserAndVideoIdForUpdate(@Param("user") User user,
    @Param("videoId") String videoId);

  // 보드에 연결된 카드들(최신순) — 기존 findByBoardId 대체용
  @Query("""
      select c
        from BoardCard bc
        join bc.card c
       where bc.board.id = :boardId
         and c.deletedAt is null
       order by c.createdAt desc
    """)
  List<Card> findByBoardIdOrderByCreatedAtDesc(@Param("boardId") Long boardId);

  // 보드에 연결된 카드 개수 — 기존 countByBoardId 대체용
  @Query("""
      select count(c)
        from BoardCard bc
        join bc.card c
       where bc.board.id = :boardId
         and c.deletedAt is null
    """)
  long countActiveByBoardId(@Param("boardId") Long boardId);

  // 썸네일용 최신 1개(또는 N개) — 기존 findTopByBoard... 대체용
  @Query("""
      select c
        from BoardCard bc
        join bc.card c
       where bc.board = :board
         and c.deletedAt is null
       order by c.createdAt desc
    """)
  List<Card> findRecentCardsByBoard(@Param("board") Board board, Pageable pageable);

  /* ============================================================
   * [추가] 동시성 제어 / 락
   * ============================================================ */

  // 삭제/수정 같은 파괴적 연산 전에 행을 잠그고 조회
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from Card c where c.id = :id")
  Optional<Card> findByIdForUpdate(@Param("id") Long id);


  /* ============================================================
   * [추가] 소프트 삭제(전역 삭제)
   * ============================================================ */

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
