//package com.nubo.domain.recommendation.service;
//
//import com.nubo.domain.board.entity.Board;
//import com.nubo.domain.board.service.BoardCardService;
//import com.nubo.domain.board.service.BoardService;
//import com.nubo.domain.card.entity.Card;
//import com.nubo.domain.card.mapper.CardMapper;
//import com.nubo.domain.card.repository.CardRepository;
//import com.nubo.domain.notification.service.FcmService;
//import com.nubo.domain.recommendation.dto.RecommendationCardResponseDto;
//import com.nubo.domain.recommendation.dto.RecommendationGroupResponseDto;
//import com.nubo.domain.recommendation.dto.RecommendationListResponseDto;
//import com.nubo.domain.recommendation.entity.RecommendationCard;
//import com.nubo.domain.recommendation.entity.RecommendationGroup;
//import com.nubo.domain.recommendation.repository.RecommendationCardRepository;
//import com.nubo.domain.recommendation.repository.RecommendationGroupRepository;
//import com.nubo.domain.recommendation.type.RecommendationGroupType;
//import com.nubo.domain.user.entity.User;
//import com.nubo.domain.user.service.UserService;
//import com.nubo.domain.video.entity.Video;
//import com.nubo.domain.video.service.VideoService;
//import com.nubo.global.error.ErrorCode;
//import com.nubo.global.error.exception.ApiException;
//import java.util.Arrays;
//import java.util.List;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class RecommendationService {
//
//  private final RecommendationCardRepository recommendationCardRepository;
//  private final UserService userService;
//  private final VideoService videoService;
//  private final CardMapper cardMapper;
//  private final CardRepository cardRepository;
//  private final BoardService boardService;
//  private final BoardCardService boardCardService;
//  private final FcmService fcmService;
//  private final RecommendationGroupRepository recommendationGroupRepository;
//
//  @Transactional(readOnly = true)
//  public RecommendationListResponseDto getRecommendations(Long userId) {
//
//    // 1. 개인 키워드 그룹 조회
//    List<RecommendationGroup> personal =
//      recommendationGroupRepository.findAllValidGroupsByUserId(userId);
//
//    // 2. 공통 인기 그룹 1개 조회
//    RecommendationGroup popular =
//      recommendationGroupRepository.findFirstByGroupTypeOrderByCreatedAtDesc(
//          RecommendationGroupType.POPULAR)
//        .orElse(null);
//
//    // Case 1: 개인 추천 있고 인기 추천도 있는 경우
//    if (!personal.isEmpty()) {
//      return RecommendationListResponseDto.builder()
//        .keywordGroups(personal.stream().map(this::toGroupDto).toList())
//        .popularGroup(popular != null ? toGroupDto(popular) : null)
//        .build();
//    }
//
//    // Case 2: 개인 추천 없음 → 인기 추천만 제공
//    return RecommendationListResponseDto.builder()
//      .keywordGroups(List.of())
//      .popularGroup(popular != null ? toGroupDto(popular) : null)
//      .build();
//  }
//
//  @Transactional
//  public Card saveRecommendedCard(Long recId, Long userId) {
//
//    // 1) 추천카드 조회
//    RecommendationCard rec = recommendationCardRepository.findById(recId)
//      .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));
//
////    if (rec.isSaved()) {
////      throw new ApiException(ErrorCode.ALREADY_SAVED_RECOMMENDATION);
////    }
//
//    User user = userService.getUserById(userId);
//
//    // 2) Video 조회
//    Video video = videoService.getVideoById(rec.getVideoId())
//      .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));
//
//    // 3) 정식 Card 생성
//    Card card = cardMapper.toEntity(user, video);
//    List<String> tags = Arrays.stream(rec.getTags().split(","))
//      .map(String::trim)
//      .filter(s -> !s.isBlank())
//      .toList();
//    card.updateMeta(rec.getSummary(), tags);
//    card.setAiCategory(rec.getAiCategory());
//
//    Card savedCard = cardRepository.save(card);
//
//    // 4) AI 카테고리 기반 실제 사용자 보드 찾기
//    Board board = boardService.getAiBoardByUserAndCategory(
//      userId,
//      rec.getAiCategory()
//    );
//
//    // 5) 보드 연결
//    boardService.ensureVisibleForUser(board.getId(), userId);
//    boardService.updateActivity(board.getId());
//    boardCardService.attachCard(board, savedCard);
//
//    // 6) 추천카드 상태 저장
//    rec.setSaved(true);
//    recommendationCardRepository.save(rec);
//
//    // 7) 알림 발송
//    try {
//      fcmService.sendCardCreatedNotification(userId, savedCard.getTitle(), savedCard);
//    } catch (Exception e) {
//      log.warn("FCM 전송 실패 - recId={}, reason={}", recId, e.getMessage());
//    }
//
//    return savedCard;
//  }
//
//  private RecommendationGroupResponseDto toGroupDto(RecommendationGroup g) {
//
//    List<RecommendationCardResponseDto> cardDtos =
//      g.getCards().stream()
//        .map(this::toCardDto)
//        .toList();
//
//    return RecommendationGroupResponseDto.builder()
//      .groupId(g.getId())
//      .groupType(g.getGroupType().name())
//      .keyword(g.getKeyword())
//      .cards(cardDtos)
//      .build();
//  }
//
//  private RecommendationCardResponseDto toCardDto(RecommendationCard c) {
//    return RecommendationCardResponseDto.builder()
//      .recCardId(c.getId())
//      .videoId(c.getVideoId())
//      .title(c.getTitle())
//      .summary(c.getSummary())
//      .thumbnailUrl(c.getThumbnailUrl())
//      .tags(
//        Arrays.stream(c.getTags().split(","))
//          .map(String::trim)
//          .filter(s -> !s.isBlank())
//          .toList()
//      )
//      .saved(c.isSaved())
//      .aiCategory(c.getAiCategory().getDisplayName())
//      .build();
//  }
//
//}
