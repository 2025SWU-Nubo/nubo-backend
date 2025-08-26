package com.nubo.domain.board.repository;

import com.nubo.domain.board.entity.BoardMember;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardMemberRepository extends JpaRepository<BoardMember, Long> {

  boolean existsByBoard_IdAndUser_Id(Long boardId, Long userId);

  @Modifying
  @Query("delete from BoardMember bm where bm.board.id in :boardIds")
  int deleteByBoardIds(@Param("boardIds") List<Long> boardIds);
}
