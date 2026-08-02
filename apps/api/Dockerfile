# ============================================
# Stage 1: BUILD — Gradle로 WAR 파일 생성
# ============================================
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /workspace

# Gradle 래퍼 + 설정 파일 먼저 복사 (의존성 캐싱)
COPY gradlew settings.gradle build.gradle ./
COPY gradle/ gradle/
COPY apps/api/build.gradle apps/api/build.gradle

# 의존성 다운로드 (소스 변경 시에도 이 레이어는 캐시 재사용)
RUN chmod +x gradlew && ./gradlew :apps:api:dependencies --no-daemon || true

    # 소스 코드 복사 후 빌드
COPY apps/api/src apps/api/src
RUN ./gradlew :apps:api:build -x test --no-daemon

# ============================================
# Stage 2: RUN — Tomcat 9에 WAR 배포
# ============================================
FROM tomcat:9-jdk17-temurin

# Tomcat 기본 앱 제거
RUN rm -rf /usr/local/tomcat/webapps/*

# WAR를 ROOT.war로 복사 (루트 컨텍스트 "/" 에 배포)
COPY --from=builder /workspace/apps/api/build/libs/api-1.0-SNAPSHOT.war \
       /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
CMD ["catalina.sh", "run"]
