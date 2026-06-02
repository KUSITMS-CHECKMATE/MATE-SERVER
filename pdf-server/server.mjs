import { chromium } from '@playwright/test';
import http from 'http';
import fs from 'fs';
import { fileURLToPath } from 'url';
import path from 'path';

const PORT = 3001;
const MATE_API_BASE_URL = (process.env.MATE_API_BASE_URL ?? 'http://localhost:8080').replace(/\/$/, '');
const __dirname = path.dirname(fileURLToPath(import.meta.url));
const HTML_PATH = path.resolve(__dirname, './stats-report.html');

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://localhost:${PORT}`);

  if (req.method === 'GET' && url.pathname === '/health') {
    res.writeHead(200, { 'Content-Type': 'text/plain' });
    res.end('ok');
    return;
  }

  if (req.method === 'GET' && url.pathname.startsWith('/fonts/')) {
    const fontFile = path.basename(url.pathname);
    const fontPath = path.resolve(__dirname, 'fonts', fontFile);
    try {
      const data = fs.readFileSync(fontPath);
      res.writeHead(200, { 'Content-Type': 'font/woff2' });
      res.end(data);
    } catch {
      res.writeHead(404);
      res.end('Font not found');
    }
    return;
  }

  if (req.method === 'GET' && url.pathname.startsWith('/img/')) {
    const imgFile = path.basename(url.pathname);
    const imgPath = path.resolve(__dirname, 'img', imgFile);
    try {
      const data = fs.readFileSync(imgPath);
      res.writeHead(200, { 'Content-Type': 'image/svg+xml' });
      res.end(data);
    } catch {
      res.writeHead(404);
      res.end('Image not found');
    }
    return;
  }

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
      const title = url.searchParams.get('title') ?? '';
      const authorization = req.headers.authorization ?? '';

      if (!testId) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'testId is required' }));
        return;
      }
      if (!authorization) {
        res.writeHead(401, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Authorization header is required' }));
        return;
      }

      console.log('[pdf-server] /generate 요청 수신', { testId, title: title || '(없음)' });

      const reportUrl = `${MATE_API_BASE_URL}/api/v1/tests/${testId}/report`;
      console.log(`[pdf-server] API 호출: ${reportUrl}`);
      const apiRes = await fetch(reportUrl, {
        headers: {
          'Content-Type': 'application/json',
          Authorization: authorization,
        },
      });
      if (!apiRes.ok) {
        throw new Error(`API 오류 ${apiRes.status}: ${await apiRes.text()}`);
      }
      const reportJson = await apiRes.json();
      console.log('[pdf-server] API 응답 data.reports 개수:', reportJson?.data?.reports?.length ?? 0);

      const resolvedTitle = title || reportJson?.data?.title || '';
      console.log('[pdf-server] 사용할 title:', resolvedTitle || '(없음)');

      const params = new URLSearchParams({ testId, ...(resolvedTitle ? { title: resolvedTitle } : {}) });
      const HTML_URL = `http://localhost:${PORT}/stats-report.html?${params.toString()}`;

      console.log(`Generating PDF for testId=${testId}...`);
      const browser = await chromium.launch({ headless: true });
      try {
        const page = await browser.newPage();

        page.on('console', msg => {
          console.log(`[page:${msg.type()}]`, msg.text());
        });
        page.on('pageerror', err => {
          console.error('[page:error]', err.message);
        });

        await page.addInitScript(`window.__REPORT_DATA__ = ${JSON.stringify(reportJson)};`);

        await page.setViewportSize({ width: 595, height: 842 });
        await page.goto(HTML_URL, { waitUntil: 'load' });

        await page.waitForSelector('[data-rendered]', { timeout: 30_000 });
        console.log('[pdf-server] 렌더링 완료 확인, PDF 생성 시작');

        const pdfBuffer = await page.pdf({
          format: 'A4',
          printBackground: true,
          margin: { top: '0', right: '0', bottom: '0', left: '0' },
        });

        const base64 = pdfBuffer.toString('base64');
        console.log(`PDF generated: ${Math.round(pdfBuffer.length / 1024)}KB`);

        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ data: base64 }));
      } finally {
        await browser.close();
      }
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
  console.log(`PDF server ready (ClusterIP only) → http://localhost:${PORT}/generate`);
  console.log(`MATE API base URL: ${MATE_API_BASE_URL}`);
});
