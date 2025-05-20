DELETE
FROM board;

ALTER TABLE board
    AUTO_INCREMENT = 11;

INSERT INTO board (id, name, board_type, source, is_shared, is_favorite, user_id, created_at,
                   updated_at)
VALUES (1, '엔터테인먼트 & 코미디', 'BOARD', 'AI', false, false, NULL, now(), now()),
       (2, '교육 & 정보', 'BOARD', 'AI', false, false, NULL, now(), now()),
       (3, '뷰티 & 패션', 'BOARD', 'AI', false, false, NULL, now(), now()),
       (4, '요리 & 라이프스타일', 'BOARD', 'AI', false, false, NULL, now(), now()),
       (5, '운동 & 건강', 'BOARD', 'AI', false, false, NULL, now(), now()),
       (6, '여행 & 브이로그', 'BOARD', 'AI', false, false, NULL, now(), now()),
       (7, '게임 & 취미', 'BOARD', 'AI', false, false, NULL, now(), now()),
       (8, '음악 & 예술', 'BOARD', 'AI', false, false, NULL, now(), now()),
       (9, 'TV & 미디어 콘텐츠', 'BOARD', 'AI', false, false, NULL, now(), now()),
       (10, '기타', 'BOARD', 'AI', false, false, NULL, now(), now());
