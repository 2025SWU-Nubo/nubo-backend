package com.nubo.domain.board.repository;

import com.nubo.domain.board.entity.BoardMember;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardMemberRepository extends JpaRepository<BoardMember, Long> {

  // 특정 보드에 해당 유저가 멤버로 존재하는지 확인
  boolean existsByBoard_IdAndUser_Id(Long boardId, Long userId);

  // 여러 보드에 속한 멤버십 일괄 삭제
  @Modifying
  @Query("delete from BoardMember bm where bm.board.id in :boardIds")
  int deleteByBoardIds(@Param("boardIds") List<Long> boardIds);

  // 특정 보드와 유저의 멤버십 조회
  Optional<BoardMember> findByBoard_IdAndUser_Id(Long boardId, Long userId);

  // 여러 보드에 대한 특정 유저의 멤버십 일괄 조회
  @Query("select bm from BoardMember bm where bm.user.id = :userId and bm.board.id in :boardIds")
  List<BoardMember> findByUserIdAndBoardIds(@Param("userId") Long userId,
    @Param("boardIds") List<Long> boardIds);

  // 특정 보드에 소속된 멤버 목록 조회용 (탈퇴 회원 제외)
  @Query("""
    SELECT bm
    FROM BoardMember bm
    WHERE bm.board.id = :boardId
      AND bm.user.deletedAt IS NULL
    """)
  List<BoardMember> findActiveMembersByBoardId(@Param("boardId") Long boardId);

  // 최근 방문한 보드 리스트 조회
  @Query("""
    SELECT bm
    FROM BoardMember bm
    JOIN FETCH bm.board b
    WHERE bm.user.id = :userId
      AND bm.lastVisitedAt IS NOT NULL
      AND b.boardType <> com.nubo.domain.board.type.BoardType.SECTION
      AND b.deletedAt IS NULL
      AND (
        b.source = 'USER'
        OR (
          b.source = 'AI'
          AND (
            EXISTS (
              SELECT 1 FROM BoardMember bm2
              WHERE bm2.board.id = b.id
                AND bm2.user.id = :userId
                AND bm2.visible = true
            )
            OR (
              NOT EXISTS (
                SELECT 1 FROM BoardMember bm2
                WHERE bm2.board.id = b.id
                  AND bm2.user.id = :userId
              )
              AND (
                EXISTS (
                  SELECT 1 FROM BoardCard bc
                  WHERE bc.board.id = b.id
                    AND bc.card.deletedAt IS NULL
                )
                OR EXISTS (
                  SELECT 1 FROM Board sb
                  WHERE sb.parentBoard.id = b.id
                    AND sb.deletedAt IS NULL
                )
              )
            )
          )
        )
      )
    ORDER BY bm.lastVisitedAt DESC
    """)
  List<BoardMember> findRecentVisitedBoards(@Param("userId") Long userId, Pageable pageable);

  // 보드 방문 시간 업데이트
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      update BoardMember bm
         set bm.lastVisitedAt = :now
       where bm.board.id = :boardId
         and bm.user.id = :userId
    """)
  void updateLastVisitedAt(@Param("boardId") Long boardId,
    @Param("userId") Long userId,
    @Param("now") LocalDateTime now);

  // =========================
  // 관심사 설정
  // =========================

  // 지정된 보드 ID들에 대해 visible 값을 true로 업데이트한다.
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
    UPDATE BoardMember bm
       SET bm.visible = true
     WHERE bm.user.id = :userId
       AND bm.board.id IN :boardIds
    """)
  int bulkSetVisibleTrue(@Param("boardIds") List<Long> boardIds,
    @Param("userId") Long userId);

  // 해당 유저의 모든 보드를 visible=false로 초기화한다.
  // 관심사 설정 전에 기본 보드 상태를 리셋하는 용도로 사용 가능
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
    UPDATE BoardMember bm
       SET bm.visible = false
     WHERE bm.user.id = :userId
       AND bm.board.source = 'AI'
       AND bm.board.boardType = 'BOARD'
    """)
  int bulkResetVisible(@Param("userId") Long userId);

  // 멤버 정렬 조회 (owner 우선)
  @Query("""
    SELECT bm
    FROM BoardMember bm
    WHERE bm.board.id = :boardId
      AND bm.user.deletedAt IS NULL
    ORDER BY 
      CASE WHEN bm.role = 'OWNER' THEN 0 ELSE 1 END,
      bm.createdAt ASC
    """)
  List<BoardMember> findAllByBoardIdOrderByRoleAndCreatedAt(Long boardId);

  // 보드 숨김 처리
  @Modifying
  @Query("""
      UPDATE BoardMember bm
      SET bm.visible = false
      WHERE bm.board.id = :boardId AND bm.user.id = :userId
    """)
  int updateVisibleFalse(@Param("boardId") Long boardId, @Param("userId") Long userId);

  @Modifying
  @Query("""
      UPDATE BoardMember bm
      SET bm.visible = true
      WHERE bm.board.id = :boardId AND bm.user.id = :userId
    """)
  int updateVisibleTrue(@Param("boardId") Long boardId, @Param("userId") Long userId);
}