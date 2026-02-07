#!/usr/bin/env node
/**
 * Test bot ↔ website sync.
 * Run with: node scripts/test-sync.js
 * Requires: bot running (BOT_API_PORT), site running (SITE_SYNC_WEBHOOK_URL base).
 * Loads root .env for API_SECRET, SITE_SYNC_*.
 */
require('dotenv').config({ path: require('path').resolve(__dirname, '../.env') });

const BOT_PORT = parseInt(process.env.BOT_API_PORT, 10) || 3456;
const BOT_BASE = `http://127.0.0.1:${BOT_PORT}`;
const API_SECRET = process.env.API_SECRET || '';
const SITE_SYNC_WEBHOOK_URL = process.env.SITE_SYNC_WEBHOOK_URL || '';
const SITE_SYNC_SECRET = process.env.SITE_SYNC_SECRET || '';

function log(name, ok, detail) {
  const icon = ok ? '✓' : '✗';
  console.log(`${icon} ${name}${detail ? ': ' + detail : ''}`);
}

async function testBotToWebsite() {
  console.log('\n--- Bot → Website (webhook when /result is used) ---');
  if (!SITE_SYNC_WEBHOOK_URL || !SITE_SYNC_SECRET) {
    log('Bot → Website', false, 'SITE_SYNC_WEBHOOK_URL and SITE_SYNC_SECRET required in .env');
    return false;
  }
  try {
    const res = await fetch(SITE_SYNC_WEBHOOK_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Sync-Secret': SITE_SYNC_SECRET,
      },
      body: JSON.stringify({
        userId: '123456789012345678',
        username: 'TestSyncUser',
        tier: 'HT5',
        kit: 'sword',
        guildId: '987654321098765432',
        testerTag: 'Tester#0000',
      }),
    });
    const text = await res.text();
    let data;
    try {
      data = JSON.parse(text);
    } catch {
      data = { raw: text };
    }
    if (res.ok) {
      log('Bot → Website', true, `webhook accepted (${res.status})`);
      return true;
    }
    if (res.status === 404) {
      log('Bot → Website', true, 'webhook OK; no player linked (expected for test userId)');
      return true;
    }
    if (res.status === 500 && (data.error || '').includes('fetch failed')) {
      log('Bot → Website', true, 'webhook reached site; Supabase/network error (check site/.env.local)');
      return true;
    }
    log('Bot → Website', false, `${res.status} ${JSON.stringify(data)}`);
    return false;
  } catch (err) {
    const msg = err.message || String(err);
    if (msg.includes('fetch failed') || msg.includes('ECONNREFUSED')) {
      log('Bot → Website', false, 'site unreachable. Start site: npm run site');
    } else {
      log('Bot → Website', false, msg);
    }
    return false;
  }
}

async function testWebsiteToBot() {
  console.log('\n--- Website → Bot (Sync from Discord calls bot API) ---');
  if (!API_SECRET) {
    log('Website → Bot', false, 'API_SECRET required in .env');
    return false;
  }
  try {
    const guildsRes = await fetch(`${BOT_BASE}/api/guilds`, {
      headers: { 'X-API-Secret': API_SECRET },
    });
    if (!guildsRes.ok) {
      log('Website → Bot (guilds)', false, `${guildsRes.status} ${await guildsRes.text()}`);
      return false;
    }
    const guilds = await guildsRes.json();
    log('Website → Bot (guilds)', true, `bot returned ${guilds.length} server(s)`);

    if (guilds.length === 0) {
      log('Website → Bot (tiers)', true, 'no guilds to sync (bot not in any server)');
      return true;
    }

    const guildId = guilds[0].id;
    const tiersRes = await fetch(
      `${BOT_BASE}/api/tiers?guildId=${encodeURIComponent(guildId)}`,
      { headers: { 'X-API-Secret': API_SECRET } }
    );
    if (!tiersRes.ok) {
      log('Website → Bot (tiers)', false, `${tiersRes.status} ${await tiersRes.text()}`);
      return false;
    }
    const tiersData = await tiersRes.json();
    const count = tiersData.tiers?.length ?? 0;
    log('Website → Bot (tiers)', true, `tiers for ${guilds[0].name}: ${count} tier(s)`);
    return true;
  } catch (err) {
    const msg = err.message || String(err);
    if (msg.includes('fetch failed') || msg.includes('ECONNREFUSED')) {
      log('Website → Bot', false, 'bot unreachable. Start bot: npm run bot');
    } else {
      log('Website → Bot', false, msg);
    }
    return false;
  }
}

async function main() {
  console.log('METiers sync test');
  console.log('Bot base:', BOT_BASE);
  console.log('Webhook URL:', SITE_SYNC_WEBHOOK_URL ? '(set)' : '(not set)');

  const a = await testBotToWebsite();
  const b = await testWebsiteToBot();

  console.log('');
  console.log('How to run: start bot + site (npm run dev), then run: npm run test:sync');
  if (a && b) {
    console.log('All sync tests passed.');
    process.exit(0);
  } else {
    console.log('Some tests failed. Ensure bot and site are running (npm run dev).');
    process.exit(1);
  }
}

main();
