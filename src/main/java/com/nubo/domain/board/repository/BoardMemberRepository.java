package com.nubo.domain.board.repository;

import com.nubo.domain.board.entity.BoardMember;
import java.util.List;
import java.util.Optional;
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
}