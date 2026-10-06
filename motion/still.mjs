import { createRequire } from 'module';
import path from 'path';
const require = createRequire('/opt/node22/lib/node_modules/');
const { chromium } = require('playwright');
const dir = path.dirname(new URL(import.meta.url).pathname);
const [sc, ...times] = process.argv.slice(2);
const b = await chromium.launch({ args: ['--allow-file-access-from-files'] });
const page = await b.newPage({ viewport: { width: 1920, height: 1080 } });
page.on('console', m => console.log('console:', m.text())); page.on('pageerror', e => console.log('ERR', e.message));
await page.goto(`file://${dir}/index.html?scene=${sc}&capture`);
await page.evaluate(() => window.__ready);
for (const t of times) { await page.evaluate(t => window.__render(+t, 0), t); await page.screenshot({ path: process.env.SP + `/s${sc}_${t}.jpg`, type: 'jpeg', quality: 70 }); }
await b.close();
