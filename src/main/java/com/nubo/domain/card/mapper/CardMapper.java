package com.nubo.domain.card.mapper;

import com.nubo.domain.board.type.BoardSource;
import com.nubo.domain.card.dto.CardDetailResponseDto;
import com.nubo.domain.card.dto.CardListResponseDto;
import com.nubo.domain.card.dto.CardResponseDto;
import com.nubo.domain.card.entity.Card;
import com.nubo.domain.user.entity.User;
import com.nubo.domain.video.entity.Video;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CardMapper {

  /* =========================
   * 엔티티 생성 (M:N 대응)
   * ========================= */

  /**
   * Card 생성: 이제 보드 주입 없음 (보드 연결은 BoardCard로 별도 처리)
   */
  public Card toEntity(User user, Video video) {
    return Card.builder()
      .title(video.getTitle())
      .user(user)
      .video(video)
      .isFavorite(false)
      .build();
  }

  /* =========================
   * 응답 매핑 (컨텍스트 기반 보드 정보 주입)
   * ========================= */

  /**
   * Card → CardResponseDto
   *
   * @param card 카드 엔티티
   */
  public CardResponseDto toResponseDto(Card card, List<Long> boardIds) {
    return CardResponseDto.builder()
      .id(card.getId())
      .title(card.getTitle())
      .summary(card.getSummary())
      .tags(splitTags(card.getTags()))
      .isFavorite(card.isFavorite())
      .videoId(card.getVideo().getId())
      .videoThumbnailUrl(card.getVideo().getThumbnailUrl())
      .boardIds(boardIds)
      .build();
  }

  /**
   * Card → CardListResponseDto
   * (리스트는 썸네일 정도만 필요하므로 보드 정보 불필요)
   */
  public CardListResponseDto toListResponseDto(Card card) {
    return CardListResponseDto.builder()
      .id(card.getId())
      .videoThumbnailUrl(card.getVideo().getThumbnailUrl())
      .build();
  }

  /**
   * Card → CardDetailResponseDto
   *
   * @param card               카드 엔티티
   * @param contextBoardName   컨텍스트 보드명(보드 상세 화면에서 호출 시)
   * @param contextBoardSource 컨텍스트 보드 소스
   *                           둘 다 null이면 보드 정보 없이 내려감
   */
  public CardDetailResponseDto toDetailResponseDto(
    Card card,
    String contextBoardName,
    BoardSource contextBoardSource
  ) {
    return CardDetailResponseDto.builder()
      .id(card.getId())
      .title(card.getTitle())
      .summary(card.getSummary())
      .tags(splitTags(card.getTags()))
      .boardSource(contextBoardSource)
      .boardName(contextBoardName)
      .videoUrl(card.getVideo().getUrl())               // 기존 필드명 유지
      .videoThumbnailUrl(card.getVideo().getThumbnailUrl())
      .videoPlatform(card.getVideo().getPlatform())
      .createdAt(card.getCreatedAt())
      .updatedAt(card.getUpdatedAt())
      .build();
  }

  /* =========================
   * 호환성 유지용 (Deprecated)
   * ========================= */

  /**
   * (구) 보드까지 받던 생성 메서드 — M:N 전환 후 사용 금지
   */
  @Deprecated
  public Card toEntity(User user, Video video, /* Board board */ Object _unused) {
    return toEntity(user, video);
  }

  /**
   * (구) 보드 필드를 Card에서 직접 읽던 매퍼 — M:N 전환 후 컨텍스트 보드로 대체
   */
  @Deprecated
  public CardResponseDto toResponseDto(Card card) {
    return toResponseDto(card, null);
  }

  /**
   * (구) 보드 필드를 Card에서 직접 읽던 매퍼 — M:N 전환 후 컨텍스트 보드로 대체
   */
  @Deprecated
  public CardDetailResponseDto toDetailResponseDto(Card card) {
    return toDetailResponseDto(card, null, null);
  }

  /* =========================
   * 유틸
   * ========================= */

  private List<String> splitTags(String tags) {
    if (tags == null || tags.isBlank()) {
      return List.of();
    }
    return Arrays.stream(tags.split(","))
      .map(String::trim)
      .filter(s -> !s.isEmpty())
      .toList();
  }
}
