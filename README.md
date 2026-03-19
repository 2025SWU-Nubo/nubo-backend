## ☁️ 프로젝트 소개
### 자기개발형 숏폼 소비자를 위한 AI 기반 지식 저장소 "Nubo"
1. 보고 지나치는 숏폼의 지식을 한곳에 모으고
2. 흩어져 있는 콘텐츠를 주제별로 정리해
3. 핵심 내용을 요약해서 편하게 다시 볼 수 있어요
4. 학습의 지속을 돕는 성장보드까지

<br>

## 🎯 프로젝트 목표
> 숏폼 컨텐츠를 **저장–분류–학습**까지 이어지도록 연결해 자기개발에 실질적으로 활용할 수 있게 돕습니다.

<br>

## 📱 실제 앱 실행 화면

| 메인 홈 | 나의 보드 | AI 요약 카드 | 카드 수정 |
| :---: | :---: | :---: | :---: |
| <img width="200" alt="image" src="https://github.com/user-attachments/assets/1e7e5353-66b1-454f-8e53-1a020790f5a2" /> | <img width="200" alt="image" src="https://github.com/user-attachments/assets/7d67d420-f5e3-4a49-8105-668a43e0b05a" /> | <img width="200" alt="image" src="https://github.com/user-attachments/assets/753e12db-e373-4fd4-bf09-07fa7038447e" /> | <img width="200" alt="image" src="https://github.com/user-attachments/assets/00f7f854-a3a3-4412-af2d-4eb5dd1bdd66" /> |
| *추천 컨텐츠* | *주제별 자동 분류* | *AI 핵심 요약* | *재가공 프롬프트 커스텀* |

| 공유 보드 | 성장 보드 | 알림 센터 | 온보딩 |
| :---: | :---: | :---: | :---: |
| <img width="200" alt="image" src="https://github.com/user-attachments/assets/2557bc22-16b7-419e-a111-1aadb6974e88" /> | <img width="200" alt="image" src="https://github.com/user-attachments/assets/af5eed26-9143-4081-995f-508f43f8e49c" /> | <img width="200" alt="image" src="https://github.com/user-attachments/assets/babe0885-5a88-4b1f-af1f-194d487e7a2f" /> | <img width="200" alt="image" src="https://github.com/user-attachments/assets/7f907971-8ce3-4bbe-af33-1fe300cca7d1" /> |
| *타 유저와의 공유* | *학습 현황 확인* | *카드 생성 완료 / 공유 보드* | *기본 보드 설정* |

<br>

## ⭐ 핵심 기능
### 1. 저장 - 영상 내용 자동 요약
    - AI를 활용한 영상 내용의 노트화
    - 핵심 키워드 추출
    - AI로 요약 노트를 재가공
### 2. 분류 - 카테고리 자동 분류
    - 사용자 관심사 기반 기본보드 세팅
    - 영상 카테고리 AI 자동 분류
    - 영상의 자동 그룹화
### 3. 학습 - 성장 보드
    - 지속적인 학습을 위한 동기 부여
    - 카드 열람 기반 학습 현황 시각화
    - 누베리 수확 기반 보상 시스템
### 4. 추천 - 관심사 기반 영상 추천
    - 사용자 맞춤형 키워드 추출 후 사용
    - 관심사 기반 새 영상 추천
    - 신규 유저는 카테고리 기반 추천

<br>

## 🛠 기술 스택

### 🧠 Language
![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Python](https://img.shields.io/badge/Python-3776AB?style=for-the-badge&logo=python&logoColor=white)
### ⚙️ Backend
![Spring Boot](https://img.shields.io/badge/SpringBoot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/JPA-59666C?style=for-the-badge)
![Spring Security](https://img.shields.io/badge/SpringSecurity-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)
### 🗄 Database
![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
### 🤖 AI / API
![OpenAI](https://img.shields.io/badge/OpenAI_API-412991?style=for-the-badge&logo=openai&logoColor=white)
![Whisper](https://img.shields.io/badge/Whisper-000000?style=for-the-badge)
![YouTube API](https://img.shields.io/badge/YouTube_Data_API-FF0000?style=for-the-badge&logo=youtube&logoColor=white)
### ☁️ DevOps / Infra
![AWS S3](https://img.shields.io/badge/AWS_S3-569A31?style=for-the-badge&logo=amazons3&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Fly.io](https://img.shields.io/badge/Fly.io-8C8C8C?style=for-the-badge)

<br>

## 🏗 시스템 아키텍처
<img width="1596" height="818" alt="image" src="https://github.com/user-attachments/assets/b095fad8-a19d-443c-8595-14fa186b668e" />

<br>

## 📊 데이터베이스 설계
<img width="1407" height="1400" alt="image" src="https://github.com/user-attachments/assets/055ec276-553e-4d8d-80dd-d29a261f5097" />

<br>

## 📁 프로젝트 구조
```text
src
 ┣ auth             # OAuth2 소셜 로그인 처리 및 JWT 토큰 발급/갱신 로직
 ┣ domain
 ┃ ┣ user           # 사용자 관리 및 프로필
 ┃ ┣ video          # 숏폼 영상 데이터 처리
 ┃ ┣ board          # 사용자별 주제 분류 및 관리
 ┃ ┣ card           # AI 요약 노트 및 핵심 키워드 저장
 ┃ ┣ stat           # 학습 현황 시각화 및 성장 데이터
 ┃ ┣ notification   # 학습 독려 리마인드 스케줄러 및 FCM 알림
 ┃ ┗ recommendation # 사용자 관심사 기반 신규 콘텐츠 추천 엔진
 ┃
 ┗ global           # 프로젝트 전역 공통 모듈
   ┣ ai             # OpenAI 연동 클라이언트 및 프롬프트 관리
   ┣ auth           # 인증/인가 관련 유틸리티
   ┣ config         # Security, JWT, Swagger 등 전역 설정
   ┣ error          # 공통 예외 처리 및 도메인별 에러 코드 정의
   ┣ jwt            # JWT 토큰 생성 및 검증 필터
   ┗ s3             # AWS S3 파일 업로드 및 관리
```

<br>

## 🔥 트러블슈팅
#### 1. 사용자 경험(UX)과 서버 자원 최적화: 작업 성격에 따른 처리 이원화
사용자 요청의 성격에 따라 Sync(동기)와 Async(비동기) 방식을 전략적으로 분리하여 적용했습니다.
  - 문제 상황: 숏폼 분석 로직(메타데이터 추출 → Whisper → GPT 요약)은 평균 40초~1분 이상의 시간이 소요되는 고부하 작업입니다. 모든 요청을 동일한 방식으로 처리할 경우 서버 부하 및 UX 저하 문제가 발생했습니다.
  - 해결 방안:
    - 단독 카드 생성 (Synchronous): 사용자가 직접 URL을 입력해 카드를 생성하는 경우, 즉각적인 결과 확인을 원하는 UX를 고려해 동기 방식으로 처리했습니다. 대신 단계별 로깅을 통해 처리 과정을 추적 가능하게 구성했습니다.
    - 추천 카드 생성 (Asynchronous): 시스템이 백그라운드에서 수행하는 대량의 추천 로직은 @Async를 활용해 비동기로 처리했습니다. 메인 스레드 점유를 방지하고, 작업 완료 시 FCM(Firebase Cloud Messaging) 푸시 알림을 발송하여 앱을 계속 켜두지 않아도 결과를 확인할 수 있도록 개선했습니다.
  - 성과: 사용자의 대기 시간을 최적화함과 동시에 고부하 작업을 백그라운드에서 안정적으로 완수하는 효율적인 구조를 구축했습니다.

#### 2. 운영 난제: yt-dlp 기반 미디어 추출 및 쿠키 세션 유지 (미해결 과제)
서비스 자동화의 핵심인 미디어 추출 과정에서 발생하는 운영상의 제약 사항입니다.
  - 문제 상황:
    - 유튜브 및 인스타그램의 봇 감지 알고리즘으로 인해 서버 IP가 빈번하게 차단되는 현상이 발생했습니다.
    - 이를 우회하기 위해 최신 로그인 쿠키를 주기적으로 수동 갱신해주어야 하며, 이는 서비스의 완전 자동화 운영에 걸림돌이 되고 있습니다.
  - 향후 개선 방향:
    - 서버 세션을 자동으로 유지하는 Headless Browser(Playwright 등) 연동
    - IP 차단 분산을 위한 Proxy Server 도입 및 로테이션 적용
    - 공식 API 혹은 유료 서드파티 미디어 추출 API 도입 검토

<br>

## 📌 향후 개선 방향
- RAG 도입: 저장된 요약 노트를 기반으로 사용자가 궁금한 점을 묻고 답하는 챗봇 기능 추가
- 커뮤니티: 공유 보드 내 사용자 소통 창구 추가 (댓글 등)
- 멀티 모달 분석: 오디오뿐만 아니라 영상 프레임 분석을 통한 요약 정확도 향상
- 자동화: 쿠키 갱신 및 추출 프로세스의 완전 자동화 구현
