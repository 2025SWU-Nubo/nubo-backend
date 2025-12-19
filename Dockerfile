FROM gradle:8.8-jdk17 AS build
WORKDIR /app

COPY build.gradle settings.gradle ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon || true

COPY . .
RUN gradle clean build -x test --no-daemon


# ─────────────────────────────
# RUN IMAGE
# ─────────────────────────────
FROM eclipse-temurin:17-jdk

WORKDIR /app

# jar 한 번만 복사
COPY --from=build /app/build/libs/*.jar app.jar

# ffmpeg + yt-dlp + nodejs 설치
RUN apt-get update && apt-get install -y ffmpeg curl nodejs \
 && ln -s /usr/bin/nodejs /usr/bin/node || true \
 && curl -L https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -o /usr/local/bin/yt-dlp \
 && chmod a+rx /usr/local/bin/yt-dlp \
 && apt-get clean && rm -rf /var/lib/apt/lists/*

#COPY cookies/cookies.txt /app/cookies.txt

ENV EXT_YTDLP_PATH=/usr/local/bin/yt-dlp
ENV EXT_FFMPEG_PATH=/usr/bin/ffmpeg
#ENV EXT_COOKIES_PATH=/app/cookies.txt

ENV SERVER_ADDRESS=0.0.0.0
ENV SERVER_PORT=8080

ENTRYPOINT ["java", "-jar", "app.jar"]