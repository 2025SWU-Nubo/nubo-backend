package com.nubo.domain.recommendation.service;

import com.nubo.domain.board.entity.Board;
import com.nubo.domain.board.service.BoardCardService;
import com.nubo.domain.board.service.BoardService;
import com.nubo.domain.board.type.DefaultBoard;
import com.nubo.domain.card.dto.CardCreateResponseDto;
import com.nubo.domain.card.entity.Card;
import com.nubo.domain.card.mapper.CardMapper;
import com.nubo.domain.card.repository.CardRepository;
import com.nubo.domain.recommendation.dto.RecommendationCardSaveRequestDto;
import com.nubo.domain.recommendation.dto.RecommendationResponseDto;
import com.nubo.domain.recommendation.entity.RecommendationCard;
import com.nubo.domain.recommendation.entity.RecommendationGroup;
import com.nubo.domain.recommendation.entity.UserSavedRecommendation;
import com.nubo.domain.recommendation.mapper.RecommendationMapper;
import com.nubo.domain.recommendation.repository.RecommendationCardRepository;
import com.nubo.domain.recommendation.repository.RecommendationGroupRepository;
import com.nubo.domain.recommendation.repository.UserSavedRecommendationRepository;
import com.nubo.domain.recommendation.type.RecommendationGroupType;
import com.nubo.domain.user.entity.User;
import com.nubo.domain.user.entity.UserInterest;
import com.nubo.domain.user.repository.UserInterestRepository;
import com.nubo.domain.user.service.UserService;
import com.nubo.domain.video.entity.Video;
import com.nubo.domain.video.service.VideoService;
import com.nubo.global.error.ErrorCode;
import com.nubo.global.error.exception.ApiException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecommendationService {

  private final UserSavedRecommendationRepository userSavedRecommendationRepository;
  private final RecommendationGroupRepository recommendationGroupRepository;
  private final RecommendationCardRepository recommendationCardRepository;
  private final UserInterestRepository userInterestRepository;
  private final CardRepository cardRepository;

  private final UserService userService;
  private final VideoService videoService;
  private final BoardService boardService;
  private final BoardCardService boardCardService;

  private final RecommendationMapper recommendationMapper;
  private final CardMapper cardMapper;

  // 하루 기준 — 새벽 5시
  private LocalDateTime today5AM() {
    return LocalDate.now().atTime(5, 0);
  }

  /**
   * 홈 화면 추천 카드 조회
   *
   * 우선순위:
   * 1) 개인 키워드 기반 추천 (KEYWORD)
   * 2) 관심사 기반 카테고리 추천 (CATEGORY + user interests) (랜덤 N개)
   * 3) 관심사 미설정 시 랜덤 카테고리 추천 (CATEGORY 전체에서 랜덤 N개)
   */
  public RecommendationResponseDto getRecommendations(Long userId) {
    // 0) 유저, 기준 시간 계산
    User user = userService.getUserById(userId);
    LocalDateTime baseTime = today5AM();

    // 1) 개인 키워드 그룹 먼저 확인
    List<RecommendationGroup> keywordGroups =
      recommendationGroupRepository.findAllByUserIdAndGroupTypeAndExpiresAtAfter(
        userId,
        RecommendationGroupType.KEYWORD,
        baseTime
      );

    if (!keywordGroups.isEmpty()) {
      return recommendationMapper.toRecommendationResponseDto(keywordGroups);
    }

    // 2) 관심사 기반 추천
    if (user.isInterestSetupCompleted()) {

      // 유저 관심사 카테고리 목록 조회
      List<DefaultBoard> interests = userInterestRepository.findAllByUserId(userId)
        .stream()
        .map(UserInterest::getCategory)
        .filter(Objects::nonNull)
        .toList();

      // 관심사가 하나도 없으면 3번 로직으로 넘어감
      if (!interests.isEmpty()) {
        List<RecommendationGroup> interestGroups =
          recommendationGroupRepository.findAllByGroupTypeAndCategoryInAndExpiresAtAfter(
            RecommendationGroupType.CATEGORY,
            interests,
            baseTime
          );

        // 관심사 중 랜덤 2개 선정
        return recommendationMapper.toRecommendationResponseDto(pickRandom(interestGroups, 2));
      }
    }

    // 3) 관심사 없음 → 랜덤 추천
    List<RecommendationGroup> categoryGroups =
      recommendationGroupRepository.findAllByGroupTypeAndExpiresAtAfter(
        RecommendationGroupType.CATEGORY,
        baseTime
      );

    List<RecommendationGroup> randomGroups = pickRandom(categoryGroups, 2);

    return recommendationMapper.toRecommendationResponseDto(randomGroups);
  }

  /**
   * 랜덤 그룹 선택
   */
  private List<RecommendationGroup> pickRandom(List<RecommendationGroup> list, int count) {
    if (list.isEmpty()) {
      return Collections.emptyList();
    }
    if (list.size() <= count) {
      return list;
    }

    List<RecommendationGroup> shuffled = new java.util.ArrayList<>(list);
    Collections.shuffle(shuffled);

    return shuffled.subList(0, count);
  }
  
  /**
   * 추천카드를 정식 카드로 저장
   */
  @Transactional
  public CardCreateResponseDto saveRecommendationCard(
    Long userId,
    RecommendationCardSaveRequestDto dto
  ) {
    // 0. 유저 조회
    User user = userService.getUserById(userId);

    // 1. 저장 여부 확인
    if (userSavedRecommendationRepository.existsByUserIdAndRecommendationCardId(userId,
      dto.getRecommendationCardId())) {
      throw new ApiException(ErrorCode.ALREADY_SAVED_RECOMMENDATION);
    }

    // 2. 추천카드/비디오 정보 조회
    RecommendationCard recCard = recommendationCardRepository.findById(
        dto.getRecommendationCardId())
      .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));

    Video video = videoService.getVideoById(recCard.getVideoId())
      .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));

    // 3. 카드 생성
    Card card = Card.builder()
      .user(user)
      .video(video)
      .title(recCard.getTitle())
      .summary(recCard.getSummary())
      .tags(recCard.getTags())
      .aiCategory(recCard.getAiCategory())
      .build();

    Card savedCard = cardRepository.save(card);

    // 4. 보드 매핑
    List<Long> boardIds = (dto.getBoardIds() != null && !dto.getBoardIds().isEmpty())
      ? dto.getBoardIds()
      : List.of(boardService
        .getAiBoardByUserAndCategory(userId, recCard.getAiCategory())
        .getId());

    for (Long boardId : boardIds) {
      Board board = boardService.getBoardById(boardId);
      boardCardService.attachCard(board, savedCard);
      boardService.ensureVisibleForUser(boardId, userId);
    }

    // 5. 저장 기록 추가
    userSavedRecommendationRepository.save(
      UserSavedRecommendation.builder()
        .user(user)
        .recommendationCard(recCard)
        .build()
    );
    List<Long> savedBoardIds = boardCardService.findBoardIdsByCardId(savedCard.getId());

    // 6. dto 반환
    return cardMapper.toCreateResponseDto(savedCard, savedBoardIds);
  }
}
