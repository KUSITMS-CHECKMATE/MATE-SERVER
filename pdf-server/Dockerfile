# Playwright 공식 이미지 (Chromium 포함)
FROM mcr.microsoft.com/playwright:v1.59.0-jammy

WORKDIR /app

# @playwright/test 설치
RUN npm install @playwright/test@1.59.1

# Pretendard 폰트 다운로드 (woff2)
RUN mkdir -p /app/fonts && \
    for weight in Regular Medium SemiBold Bold; do \
      curl -fL "https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/packages/pretendard/dist/web/static/woff2/Pretendard-${weight}.woff2" \
           -o "/app/fonts/Pretendard-${weight}.woff2"; \
    done

# 서버 파일 및 HTML 템플릿 복사 (같은 폴더에 위치)
COPY server.mjs ./
COPY stats-report.html ./
COPY img/ ./img/

EXPOSE 3001

CMD ["node", "server.mjs"]
