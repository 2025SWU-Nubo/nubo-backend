package com.nubo.domain.board.controller;

import com.nubo.domain.board.dto.BoardCreateRequestDto;
import com.nubo.domain.board.dto.BoardDetailResponseDto;
import com.nubo.domain.board.dto.BoardListResponseDto;
import com.nubo.domain.board.dto.BoardResponseDto;
import com.nubo.domain.board.service.BoardService;
import com.nubo.global.auth.UserUtil;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/board")
public class BoardController {

  private final BoardService boardService;
  private final UserUtil userUtil;

  /**
   * 로그인한 사용자의 새 보드 또는 섹션을 생성한다.
   *
   * @param dto 생성할 보드 정보 DTO
   * @return 생성된 보드 정보 DTO
   */
  @PostMapping
  public BoardResponseDto createBoard(@RequestBody @Valid BoardCreateRequestDto dto) {
    Long userId = userUtil.getAuthenticatedUserId();
    return boardService.createBoard(dto, userId);
  }

  /**
   * 로그인한 사용자의 보드 목록을 조회한다. (1차 분류만)
   *
   * @return 사용자의 보드 리스트
   */
  @GetMapping
  public List<BoardListResponseDto> getUserBoards() {
    Long userId = userUtil.getAuthenticatedUserId();
    return boardService.getUserBoards(userId);
  }

  /**
   * 보드 ID로 상세 정보를 조회한다.
   *
   * @param boardId 조회할 보드 ID
   * @return 보드 상세 정보 응답 DTO
   */
  @GetMapping("/{boardId}")
  public ResponseEntity<BoardDetailResponseDto> getBoardDetail(@PathVariable Long boardId) {
    BoardDetailResponseDto detail = boardService.getBoardDetail(boardId);
    return ResponseEntity.ok(detail);
  }
}
