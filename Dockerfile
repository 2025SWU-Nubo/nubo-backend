FROM gradle:8.8-jdk17 AS build
WORKDIR /app

COPY build.gradle settings.gradle ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon || true

COPY . .
RUN gradle clean build -x test --no-daemon

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    ffmpeg \
 && pip install -U yt-dlp \
 && apt-get clean \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

ENV EXT_YTDLP_PATH=/usr/local/bin/yt-dlp
ENV EXT_FFMPEG_PATH=/usr/bin/ffmpeg

CMD ["java", "-jar", "app.jar"]