import { chromium } from '@playwright/test';
import http from 'http';
import fs from 'fs';
import { fileURLToPath } from 'url';
import path from 'path';

const PORT = 3001;
const __dirname = path.dirname(fileURLToPath(import.meta.url));
const HTML_PATH = path.resolve(__dirname, './stats-report.html'); // 같은 폴더에 위치

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://localhost:${PORT}`);

  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  // stats-report.html을 HTTP로 서빙 (file:// 대신 http:// 사용해야 쿼리 파라미터가 정상 동작)
  if (req.method === 'GET' && url.pathname === '/stats-report.html') {
    try {
      const html = fs.readFileSync(HTML_PATH, 'utf-8');
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
      res.end(html);
    } catch (error) {
      res.writeHead(500);
      res.end('Failed to read HTML');
    }
    return;
  }

  if (req.method === 'GET' && url.pathname === '/generate') {
    try {
      const testId = url.searchParams.get('testId') ?? '';
      const token = url.searchParams.get('token') ?? '';
      const apiBase = url.searchParams.get('apiBase') ?? '';
      const title = url.searchParams.get('title') ?? '';

      console.log('[pdf-server] /generate 요청 수신');
      console.log('[pdf-server] 받은 params:', { testId, token: token ? '있음' : '없음', apiBase, title });

      const params = new URLSearchParams({ testId, token, apiBase, ...(title ? { title } : {}) });
      // http:// 로 서빙해야 location.search 쿼리 파라미터가 정상 동작함
      const HTML_URL = `http://localhost:${PORT}/stats-report.html?${params.toString()}`;

      console.log('[pdf-server] HTML URL:', HTML_URL);

      // Node.js에서 직접 API 호출 (CORS 없음)
      console.log(`[pdf-server] API 호출: ${apiBase}/api/v1/tests/${testId}/report`);
      const apiRes = await fetch(`${apiBase}/api/v1/tests/${testId}/report`, {
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
      });
      if (!apiRes.ok) {
        throw new Error(`API 오류 ${apiRes.status}: ${await apiRes.text()}`);
      }
      const reportJson = await apiRes.json();
      console.log('[pdf-server] API 응답 data.reports 개수:', reportJson?.data?.reports?.length ?? 0);

      console.log(`Generating PDF for testId=${testId}...`);
      const browser = await chromium.launch({ headless: true });
      const page = await browser.newPage();

      // 브라우저 콘솔 로그를 터미널에 출력
      page.on('console', msg => {
        console.log(`[page:${msg.type()}]`, msg.text());
      });
      page.on('pageerror', err => {
        console.error('[page:error]', err.message);
      });

      // API 데이터를 전역 변수로 주입 (브라우저 CORS 우회)
      await page.addInitScript(`window.__REPORT_DATA__ = ${JSON.stringify(reportJson)};`);

      await page.setViewportSize({ width: 595, height: 842 });
      await page.goto(HTML_URL, { waitUntil: 'load' });

      // async 렌더링이 완전히 끝날 때까지 대기
      await page.waitForSelector('[data-rendered]', { timeout: 30_000 });
      console.log('[pdf-server] 렌더링 완료 확인, PDF 생성 시작');

      const pdfBuffer = await page.pdf({
        format: 'A4',
        printBackground: true,
        margin: { top: '0', right: '0', bottom: '0', left: '0' },
      });

      await browser.close();

      const base64 = pdfBuffer.toString('base64');
      console.log(`PDF generated: ${Math.round(pdfBuffer.length / 1024)}KB`);

      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ data: base64 }));
    } catch (error) {
      console.error('PDF generation error:', error);
      res.writeHead(500, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: error.message }));
    }
    return;
  }

  res.writeHead(404);
  res.end('Not found');
});

server.listen(PORT, () => {
  console.log(`PDF server ready → http://localhost:${PORT}/generate`);
});
