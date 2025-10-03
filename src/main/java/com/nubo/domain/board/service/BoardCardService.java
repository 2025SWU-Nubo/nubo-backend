package com.nubo.domain.board.service;

import com.nubo.domain.board.entity.Board;
import com.nubo.domain.board.entity.BoardCard;
import com.nubo.domain.board.repository.BoardCardRepository;
import com.nubo.domain.card.entity.Card;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BoardCardService {

  private final BoardCardRepository boardCardRepository;

  @Transactional(readOnly = true)
  public List<Long> findBoardIdsByCardId(Long cardId) {
    return boardCardRepository.findBoardIdsByCardId(cardId);
  }

  @Transactional(readOnly = true)
  public boolean existsLink(Long boardId, Long cardId) {
    return boardCardRepository.existsByBoard_IdAndCard_Id(boardId, cardId);
  }

  @Transactional
  public void detachCard(Long boardId, Long cardId) {
    boardCardRepository.deleteByBoardIdAndCardId(boardId, cardId);
  }

  @Transactional
  public int detachByBoardIds(List<Long> boardIds) {
    return boardCardRepository.deleteByBoardIds(boardIds);
  }

  @Transactional(readOnly = true)
  public List<Long> findDistinctCardIdsByBoardIds(List<Long> boardIds) {
    return boardCardRepository.findDistinctCardIdsByBoardIds(boardIds);
  }

  @Transactional
  public void attachCard(Board board, Card card) {
    if (!existsLink(board.getId(), card.getId())) {
      BoardCard link = BoardCard.builder()
        .board(board)
        .card(card)
        .build();
      boardCardRepository.save(link);
    }
  }

  public List<BoardCard> getByBoardId(Long boardId) {
    return boardCardRepository.findByBoardId(boardId);
  }

  public boolean exists(Long boardId, Long cardId) {
    return boardCardRepository.existsByBoardIdAndCardId(boardId, cardId);
  }

  public boolean existsByTitleInBoard(Long boardId, String title) {
    return boardCardRepository.existsByBoardIdAndCardTitle(boardId, title);
  }

  public void add(Board board, Card card) {
    BoardCard bc = BoardCard.builder()
      .board(board)
      .card(card)
      .build();
    boardCardRepository.save(bc);
  }
}
