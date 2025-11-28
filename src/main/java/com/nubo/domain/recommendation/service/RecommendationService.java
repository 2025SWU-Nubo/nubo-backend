package com.nubo.domain.recommendation.service;

import com.nubo.domain.board.entity.Board;
import com.nubo.domain.board.service.BoardCardService;
import com.nubo.domain.board.service.BoardService;
import com.nubo.domain.board.type.DefaultBoard;
import com.nubo.domain.card.dto.CardCreateResponseDto;
import com.nubo.domain.card.entity.Card;
import com.nubo.domain.card.mapper.CardMapper;
import com.nubo.domain.card.repository.CardRepository;
import com.nubo.domain.recommendation.dto.RecommendationCardDetailResponseDto;
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
import java.util.ArrayList;
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
    String nickname = user.getNickname();
    LocalDateTime baseTime = today5AM();
    final int RANDOM_PICK_COUNT = 2; // 랜덤으로 뽑을 그룹 수 상수화

    // 이미 저장된 카드 ID 목록을 미리 조회
    List<Long> savedCardIds = userSavedRecommendationRepository
      .findSavedRecommendationCardIds(userId);

    List<RecommendationGroup> resultGroups = new ArrayList<>();

    // 1) 개인 키워드 그룹 확인 (최우선)
    resultGroups = getKeywordRecommendations(userId, baseTime, savedCardIds);
    if (!resultGroups.isEmpty()) {
      return recommendationMapper.toRecommendationResponseDto(resultGroups, nickname);
    }

    // 2) 관심사 기반 추천 확인
    if (user.isInterestSetupCompleted()) {
      resultGroups = getInterestRecommendations(user, baseTime, savedCardIds, RANDOM_PICK_COUNT);
      if (!resultGroups.isEmpty()) {
        return recommendationMapper.toRecommendationResponseDto(resultGroups, nickname);
      }
    }

    // 3) 관심사 없음 → 랜덤 카테고리 추천 (Fallback)
    resultGroups = getRandomRecommendations(baseTime, savedCardIds, RANDOM_PICK_COUNT);

    return recommendationMapper.toRecommendationResponseDto(resultGroups, nickname);
  }


  /**
   * 1) 개인 키워드 기반 추천 그룹을 조회하고 저장된 카드를 필터링한다.
   */
  private List<RecommendationGroup> getKeywordRecommendations(
    Long userId,
    LocalDateTime baseTime,
    List<Long> savedCardIds
  ) {
    List<RecommendationGroup> keywordGroups =
      recommendationGroupRepository.findAllByUserIdAndGroupTypeAndExpiresAtAfter(
        userId,
        RecommendationGroupType.KEYWORD,
        baseTime
      );

    // 필터링 후, 카드가 남아 있는 그룹만 반환 (카드가 모두 저장된 그룹은 제외)
    return filterGroups(keywordGroups, savedCardIds).stream()
      .filter(group -> !group.getCards().isEmpty()) // 카드가 남아있는 그룹만 최종 선택
      .toList();
  }

  /**
   * 2) 관심사 기반 추천 그룹을 조회하고 저장된 카드를 필터링한다.
   */
  private List<RecommendationGroup> getInterestRecommendations(
    User user,
    LocalDateTime baseTime,
    List<Long> savedCardIds,
    int pickCount
  ) {
    // 유저 관심사 카테고리 목록 조회
    List<DefaultBoard> interests = userInterestRepository.findAllByUserId(user.getId()).stream()
      .map(UserInterest::getCategory)
      .filter(Objects::nonNull)
      .toList();

    if (interests.isEmpty()) {
      return List.of(); // 관심사가 없으면 빈 리스트 반환
    }

    List<RecommendationGroup> interestGroups =
      recommendationGroupRepository.findAllByGroupTypeAndCategoryInAndExpiresAtAfter(
        RecommendationGroupType.CATEGORY,
        interests,
        baseTime
      );

    List<RecommendationGroup> randomGroups = pickRandom(interestGroups, pickCount);

    // 필터링 후, 카드가 남아 있는 그룹만 반환 (카드가 모두 저장된 그룹은 제외)
    return filterGroups(randomGroups, savedCardIds).stream()
      .filter(group -> !group.getCards().isEmpty()) // 카드가 남아있는 그룹만 최종 선택
      .toList();
  }

  /**
   * 3) 랜덤 카테고리 추천 그룹을 조회하고 저장된 카드를 필터링한다. (Fallback)
   */
  private List<RecommendationGroup> getRandomRecommendations(
    LocalDateTime baseTime,
    List<Long> savedCardIds,
    int pickCount
  ) {
    List<RecommendationGroup> categoryGroups =
      recommendationGroupRepository.findAllByGroupTypeAndExpiresAtAfter(
        RecommendationGroupType.CATEGORY,
        baseTime
      );

    List<RecommendationGroup> randomGroups = pickRandom(categoryGroups, pickCount);

    // 필터링 후, 카드가 남아 있는 그룹만 반환 (Fallback은 카드가 없어도 반환할 수 있으나,
    // 원본 로직을 따라 카드가 있는 그룹만 반환하는 것으로 가정)
    return filterGroups(randomGroups, savedCardIds);
  }

  /**
   * 헬퍼 메서드: 각 그룹 내에서 이미 저장된 카드를 제거하고, 카드 목록을 업데이트한다. (조회에서만 숨김처리)
   */
  private List<RecommendationGroup> filterGroups(
    List<RecommendationGroup> groups,
    List<Long> savedCardIds
  ) {
    return groups.stream()
      .map(group -> {
        var filtered = group.getCards().stream()
          .filter(c -> !savedCardIds.contains(c.getId()))
          .toList();
        group.setCards(filtered); // 그룹 객체의 카드 목록 업데이트
        return group;
      })
      .toList();
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
   * 특정 ID의 추천 카드를 조회한다.
   *
   * @param cardId 카드 ID
   * @return 추천카드 응답 DTO
   * @exception ApiException 카드가 존재하지 않으면 예외 발생
   */
  @Transactional
  public RecommendationCardDetailResponseDto getRecommendationCardById(Long cardId) {
    RecommendationCard recommendationCard = recommendationCardRepository.findById(cardId)
      .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));
    return recommendationMapper.toDetailResponseDto(recommendationCard);
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

    Video video = videoService.getVideoById(recCard.getVideo().getId())
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
