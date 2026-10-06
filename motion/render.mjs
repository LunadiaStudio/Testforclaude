// 사용법: node render.mjs [scene번호...]  → out/sceneN.mp4
import { createRequire } from 'module';
import { spawn } from 'child_process';
import path from 'path';
const require = createRequire('/opt/node22/lib/node_modules/');
const { chromium } = require('playwright');
const FPS = 30;
const dir = path.dirname(new URL(import.meta.url).pathname);
const scenes = process.argv.slice(2).length ? process.argv.slice(2) : ['1', '2', '3'];
const browser = await chromium.launch({ args: ['--allow-file-access-from-files'] });
for (const sc of scenes) {
  const page = await browser.newPage({ viewport: { width: 1920, height: 1080 } });
  await page.goto(`file://${dir}/index.html?scene=${sc}&capture`);
  await page.evaluate(() => window.__ready);
  const dur = await page.evaluate(() => window.DUR);
  const n = Math.round(dur * FPS);
  const out = `${dir}/out/scene${sc}.mp4`;
  const ff = spawn('ffmpeg', ['-y', '-loglevel', 'error', '-f', 'image2pipe', '-framerate', String(FPS), '-c:v', 'mjpeg', '-i', '-',
    '-c:v', 'libx264', '-preset', 'slow', '-crf', '16', '-pix_fmt', 'yuv420p', '-movflags', '+faststart', out], { stdio: ['pipe', 'inherit', 'inherit'] });
  for (let f = 0; f < n; f++) {
    await page.evaluate(([t, f]) => window.__render(t, f), [f / FPS, f]);
    const buf = await page.screenshot({ type: 'jpeg', quality: 95 });
    if (!ff.stdin.write(buf)) await new Promise(r => ff.stdin.once('drain', r));
  }
  ff.stdin.end();
  await new Promise(r => ff.on('close', r));
  console.log(`scene${sc}: ${n} frames → ${out}`);
  await page.close();
}
await browser.close();
