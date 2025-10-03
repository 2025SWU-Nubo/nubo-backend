package com.nubo.domain.board.repository;

import com.nubo.domain.board.entity.BoardCard;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardCardRepository extends JpaRepository<BoardCard, BoardCard.BoardCardId> {

  // 카드가 연결된 보드 ID 목록 조회
  @Query("select bc.board.id from BoardCard bc where bc.card.id = :cardId")
  List<Long> findBoardIdsByCardId(@Param("cardId") Long cardId);

  // 여러 보드(트리) 아래의 카드 ID 목록 조회
  @Query("""
      select distinct bc.card.id
      from BoardCard bc
      where bc.board.id in :boardIds
    """)
  List<Long> findDistinctCardIdsByBoardIds(Collection<Long> boardIds);

  // 보드-카드 링크 존재 여부 확인
  boolean existsByBoard_IdAndCard_Id(Long boardId, Long cardId);

  // 특정 보드에서 카드 하나 제거
  @Modifying
  @Query("delete from BoardCard bc where bc.board.id = :boardId and bc.card.id = :cardId")
  int deleteByBoardIdAndCardId(@Param("boardId") Long boardId,
    @Param("cardId") Long cardId);


  // 여러 보드(트리) 아래의 보드-카드 링크 일괄 삭제
  @Modifying
  @Query("delete from BoardCard bc where bc.board.id in :boardIds")
  int deleteByBoardIds(Collection<Long> boardIds);

  // --- 벌크 액션 ---
  // 보드 아이디로 연관관계 조회
  List<BoardCard> findByBoardId(Long boardId);

  // 보드/카드 아이디로 존재여부 반환
  boolean existsByBoardIdAndCardId(Long boardId, Long cardId);

  // 보드 아이디 + 카드 이름으로 존재여부 반환
  @Query("SELECT CASE WHEN COUNT(bc) > 0 THEN true ELSE false END " +
    "FROM BoardCard bc " +
    "WHERE bc.board.id = :boardId AND bc.card.title = :title")
  boolean existsByBoardIdAndCardTitle(@Param("boardId") Long boardId, @Param("title") String title);

}