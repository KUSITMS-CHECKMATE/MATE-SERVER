# Playwright 공식 이미지 (Chromium 포함)
FROM mcr.microsoft.com/playwright:v1.59.0-jammy

WORKDIR /app

# @playwright/test 설치
RUN npm install @playwright/test@1.59.1

# 서버 파일 및 HTML 템플릿 복사 (같은 폴더에 위치)
COPY server.mjs ./
COPY stats-report.html ./

EXPOSE 3001

CMD ["node", "server.mjs"]
