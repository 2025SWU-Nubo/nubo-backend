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

  // 특정 보드에 소속된 멤버 목록 조회용
  List<BoardMember> findAllByBoardId(Long boardId);

  // 최근 방문한 보드 리스트 조회
  @Query("""
    SELECT bm
    FROM BoardMember bm
    JOIN FETCH bm.board b
    WHERE bm.user.id = :userId
      AND bm.lastVisitedAt IS NOT NULL
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
  int updateLastVisitedAt(@Param("boardId") Long boardId,
    @Param("userId") Long userId,
    @Param("now") LocalDateTime now);
}