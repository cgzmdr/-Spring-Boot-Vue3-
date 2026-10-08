// Capture screenshots (and page text for verification) via Chrome DevTools Protocol.
// Usage: node shoot.mjs <configJsonPath>
// Config: { port, shots: [ { url, out, width, height, fullPage, waitMs, evalWait, scrollY, click } ] }
import fs from 'node:fs';
import path from 'node:path';

const cfgPath = process.argv[2];
const cfg = JSON.parse(fs.readFileSync(cfgPath, 'utf8').replace(/^\uFEFF/, ''));
const port = cfg.port || 9222;

async function getJson(url) {
  const r = await fetch(url);
  return await r.json();
}

class CDP {
  constructor(ws) {
    this.ws = ws;
    this.id = 0;
    this.pending = new Map();
    this.events = [];
    ws.addEventListener('message', (ev) => {
      const msg = JSON.parse(ev.data);
      if (msg.id && this.pending.has(msg.id)) {
        const { resolve, reject } = this.pending.get(msg.id);
        this.pending.delete(msg.id);
        if (msg.error) reject(new Error(JSON.stringify(msg.error)));
        else resolve(msg.result);
      } else if (msg.method) {
        for (const e of this.events) e(msg);
      }
    });
  }
  send(method, params = {}) {
    const id = ++this.id;
    return new Promise((resolve, reject) => {
      this.pending.set(id, { resolve, reject });
      this.ws.send(JSON.stringify({ id, method, params }));
      setTimeout(() => {
        if (this.pending.has(id)) {
          this.pending.delete(id);
          reject(new Error('timeout ' + method));
        }
      }, 60000);
    });
  }
  once(method, timeout = 20000) {
    return new Promise((resolve) => {
      const t = setTimeout(() => {
        this.events = this.events.filter((f) => f !== fn);
        resolve(null);
      }, timeout);
      const fn = (msg) => {
        if (msg.method === method) {
          clearTimeout(t);
          this.events = this.events.filter((f) => f !== fn);
          resolve(msg.params);
        }
      };
      this.events.push(fn);
    });
  }
  async evalJson(expr) {
    const r = await this.send('Runtime.evaluate', {
      expression: `JSON.stringify((()=>{ ${expr} })())`,
      returnByValue: true,
      awaitPromise: true,
    });
    if (r.exceptionDetails) throw new Error('eval error: ' + JSON.stringify(r.exceptionDetails).slice(0, 500));
    const v = r.result.value;
    return v === undefined ? undefined : JSON.parse(v);
  }
}

const report = { shots: [], failures: [] };

for (const shot of cfg.shots) {
  let target;
  try {
    const created = await fetch(`http://127.0.0.1:${port}/json/new?about:blank`, { method: 'PUT' });
    target = await created.json();
  } catch (e) {
    report.failures.push({ url: shot.url, error: 'cannot create target: ' + e.message });
    continue;
  }
  const ws = new WebSocket(target.webSocketDebuggerUrl);
  await new Promise((res, rej) => {
    ws.addEventListener('open', res);
    ws.addEventListener('error', rej);
  });
  const cdp = new CDP(ws);
  try {
    await cdp.send('Page.enable');
    await cdp.send('Runtime.enable');
    const width = shot.width || 1600;
    const height = shot.height || 950;
    await cdp.send('Emulation.setDeviceMetricsOverride', {
      width,
      height,
      deviceScaleFactor: shot.dsf || 1,
      mobile: !!shot.mobile,
    });
    const loaded = cdp.once('Page.loadEventFired', 30000);
    await cdp.send('Page.navigate', { url: shot.url });
    await loaded;
    // wait for SPA data / animations
    await new Promise((r) => setTimeout(r, shot.waitMs ?? 2500));
    if (shot.evalWait) {
      const deadline = Date.now() + 20000;
      let ok = false;
      while (Date.now() < deadline) {
        try {
          ok = await cdp.evalJson(`return !!(${shot.evalWait});`);
        } catch (e) {
          ok = false;
        }
        if (ok) break;
        await new Promise((r) => setTimeout(r, 400));
      }
      if (!ok) report.failures.push({ url: shot.url, error: 'evalWait not satisfied: ' + shot.evalWait });
    }
    if (shot.click) {
      await cdp.evalJson(`const el=document.querySelector(${JSON.stringify(shot.click)}); if(el) el.click(); return !!el;`);
      await new Promise((r) => setTimeout(r, shot.afterClickMs ?? 1500));
    }
    if (shot.scrollY) {
      await cdp.evalJson(`window.scrollTo(0, ${Number(shot.scrollY)}); return window.scrollY;`);
      await new Promise((r) => setTimeout(r, 700));
    }
    if (shot.hash) {
      await cdp.evalJson(`location.hash=${JSON.stringify(shot.hash)}; return location.hash;`);
      await new Promise((r) => setTimeout(r, shot.afterHashMs ?? 2500));
    }
    if (Array.isArray(shot.steps)) {
      for (const st of shot.steps) {
        if (st.hash) {
          await cdp.evalJson(`location.hash=${JSON.stringify(st.hash)}; return location.hash;`);
        }
        if (st.navigate) {
          const l = cdp.once('Page.loadEventFired', 30000);
          await cdp.send('Page.navigate', { url: st.navigate });
          await l;
        }
        if (st.eval) {
          try {
            const r = await cdp.evalJson(st.eval);
            if (st.expect !== undefined && r !== st.expect) {
              report.failures.push({ url: shot.url, error: `step expect ${JSON.stringify(st.expect)} got ${JSON.stringify(r)} for ${st.eval}` });
            }
          } catch (e) {
            report.failures.push({ url: shot.url, error: 'soft step error: ' + e.message.slice(0, 200) });
          }
        }
        await new Promise((r) => setTimeout(r, st.waitMs ?? 1200));
      }
    }
    const info = await cdp.evalJson(`
      const b = document.body;
      const txt = (b ? b.innerText : '').replace(/\\s+/g,' ').trim();
      return {
        title: document.title,
        textLen: txt.length,
        textHead: txt.slice(0, 400),
        scrollW: document.documentElement.scrollWidth,
        scrollH: document.documentElement.scrollHeight,
        imgs: document.images.length,
        imgsLoaded: Array.from(document.images).filter(i => i.complete && i.naturalWidth > 0).length
      };
    `);
    if (shot.probes) {
      info.probes = {};
      for (const [k, expr] of Object.entries(shot.probes)) {
        try {
          info.probes[k] = await cdp.evalJson(expr);
        } catch (e) {
          info.probes[k] = 'ERR ' + e.message;
        }
      }
    }
    let clip;
    if (shot.fullPage) {
      const m = await cdp.send('Page.getLayoutMetrics');
      const cs = m.cssContentSize || m.contentSize;
      clip = { x: 0, y: 0, width: Math.ceil(cs.width), height: Math.ceil(Math.min(cs.height, shot.maxHeight || 5000)), scale: 1 };
    }
    const png = await cdp.send('Page.captureScreenshot', {
      format: 'png',
      captureBeyondViewport: !!shot.fullPage,
      ...(clip ? { clip } : {}),
    });
    const outPath = path.resolve(shot.out);
    fs.mkdirSync(path.dirname(outPath), { recursive: true });
    fs.writeFileSync(outPath, Buffer.from(png.data, 'base64'));
    report.shots.push({ url: shot.url, out: shot.out, bytes: fs.statSync(outPath).size, ...info });
  } catch (e) {
    report.failures.push({ url: shot.url, error: e.message });
  } finally {
    try {
      ws.close();
    } catch {}
    try {
      await fetch(`http://127.0.0.1:${port}/json/close/${target.id}`);
    } catch {}
  }
}

fs.writeFileSync(cfg.reportPath || 'report.json', JSON.stringify(report, null, 1), 'utf8');
console.log(JSON.stringify(report, null, 1));
