require('dotenv').config({ path: require('path').join(__dirname, '..', '.env') });
const express = require('express');
const session = require('express-session');
const path = require('path');
const crypto = require('crypto');
const fetch = require('node-fetch');

const app = express();
const WEB_PORT = parseInt(process.env.WEB_PORT, 10) || 3457;
const BOT_API = `http://127.0.0.1:${process.env.BOT_API_PORT || 3456}`;
const API_SECRET = process.env.API_SECRET || '';

app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(
  session({
    secret: process.env.SESSION_SECRET || 'metiers-dashboard-secret',
    resave: false,
    saveUninitialized: false,
    cookie: { maxAge: 7 * 24 * 60 * 60 * 1000 },
  })
);

function requireAuth(req, res, next) {
  if (process.env.DASHBOARD_PASSWORD && !req.session.authenticated) {
    return res.redirect('/login');
  }
  next();
}

app.get('/', (req, res, next) => {
  if (process.env.DASHBOARD_PASSWORD && !req.session.authenticated) {
    return res.redirect('/login');
  }
  next();
});
app.use(express.static(path.join(__dirname, 'public')));

app.get('/login', (req, res) => {
  if (!process.env.DASHBOARD_PASSWORD) return res.redirect('/');
  res.sendFile(path.join(__dirname, 'public', 'login.html'));
});

app.post('/login', (req, res) => {
  const pw = process.env.DASHBOARD_PASSWORD;
  if (!pw) return res.redirect('/login?error=1');
  const input = req.body.password || '';
  if (typeof input !== 'string' || input.length !== pw.length ||
      !crypto.timingSafeEqual(Buffer.from(input), Buffer.from(pw))) {
    return res.redirect('/login?error=1');
  }
  req.session.authenticated = true;
  res.redirect('/');
});

app.post('/logout', (req, res) => {
  req.session.destroy(() => res.redirect(process.env.DASHBOARD_PASSWORD ? '/login' : '/'));
});
app.get('/logout', (req, res) => {
  req.session.destroy(() => res.redirect(process.env.DASHBOARD_PASSWORD ? '/login' : '/'));
});

async function botApi(method, path, body) {
  const url = `${BOT_API}${path}`;
  const opts = {
    method,
    headers: {
      'Content-Type': 'application/json',
      'X-API-Secret': API_SECRET,
    },
  };
  if (body && (method === 'POST' || method === 'PUT' || method === 'PATCH')) opts.body = JSON.stringify(body);
  const res = await fetch(url, opts);
  const text = await res.text();
  let data;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = null;
  }
  if (!res.ok) throw new Error(data?.error || res.statusText || 'Request failed');
  return data;
}

app.get('/api/guilds', requireAuth, async (req, res) => {
  try {
    const data = await botApi('GET', '/api/guilds');
    res.json(data);
  } catch (e) {
    res.status(502).json({ error: e.message || 'Bot unreachable' });
  }
});

app.get('/api/guilds/:id/channels', requireAuth, async (req, res) => {
  try {
    const data = await botApi('GET', `/api/guilds/${req.params.id}/channels`);
    res.json(data);
  } catch (e) {
    res.status(502).json({ error: e.message || 'Bot unreachable' });
  }
});

app.get('/api/guilds/:id/roles', requireAuth, async (req, res) => {
  try {
    const data = await botApi('GET', `/api/guilds/${req.params.id}/roles`);
    res.json(data);
  } catch (e) {
    res.status(502).json({ error: e.message || 'Bot unreachable' });
  }
});

app.get('/api/guilds/:id/categories', requireAuth, async (req, res) => {
  try {
    const data = await botApi('GET', `/api/guilds/${req.params.id}/categories`);
    res.json(data);
  } catch (e) {
    res.status(502).json({ error: e.message || 'Bot unreachable' });
  }
});

app.get('/api/panels', requireAuth, async (req, res) => {
  const guildId = req.query.guildId;
  if (!guildId) return res.status(400).json({ error: 'guildId required' });
  try {
    const data = await botApi('GET', `/api/panels?guildId=${guildId}`);
    res.json(data);
  } catch (e) {
    res.status(502).json({ error: e.message || 'Bot unreachable' });
  }
});

app.get('/api/panels/:id', requireAuth, async (req, res) => {
  try {
    const data = await botApi('GET', `/api/panels/${req.params.id}`);
    res.json(data);
  } catch (e) {
    res.status(e.message?.includes('not found') ? 404 : 502).json({ error: e.message || 'Failed' });
  }
});

app.post('/api/panels', requireAuth, async (req, res) => {
  try {
    const data = await botApi('POST', '/api/panels', req.body);
    res.status(201).json(data);
  } catch (e) {
    res.status(e.message.includes('Maximum') ? 400 : 502).json({ error: e.message || 'Failed' });
  }
});

app.patch('/api/panels/:id', requireAuth, async (req, res) => {
  try {
    const data = await botApi('PATCH', `/api/panels/${req.params.id}`, req.body);
    res.json(data);
  } catch (e) {
    res.status(502).json({ error: e.message || 'Bot unreachable' });
  }
});

app.delete('/api/panels/:id', requireAuth, async (req, res) => {
  try {
    await botApi('DELETE', `/api/panels/${req.params.id}`);
    res.json({ ok: true });
  } catch (e) {
    res.status(502).json({ error: e.message || 'Bot unreachable' });
  }
});

app.listen(WEB_PORT, () => {
  console.log(`METiers dashboard: http://localhost:${WEB_PORT}`);
});
