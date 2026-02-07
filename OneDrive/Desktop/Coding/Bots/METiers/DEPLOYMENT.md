# METiers Deployment Guide

## Project Structure

```
metiers/
├── bot/              # Discord bot files
├── site/             # Next.js website
├── data/             # SQLite database (auto-created)
├── .env              # Bot environment variables
├── .env.example      # Template for .env
└── package.json      # Bot dependencies
```

---

## 1. Discord Bot Deployment

### Files to Upload (Bot Hosting - e.g., Railway, Render, VPS)

Upload the **entire root folder** excluding:
- `node_modules/` (will be installed)
- `site/` (hosted separately)
- `.env` (configure on host)
- `data/` (auto-created)
- `.claude/`

**Required files:**
```
bot/
  ├── index.js        # Main bot entry
  ├── db.js           # Database functions
  ├── tester.js       # /result, /set, /tiers commands
  ├── commands.js     # Slash commands
  ├── panels.js       # Ticket panel builders
  ├── transcript.js   # HTML transcript generator
  ├── api.js          # Express API server
  └── logs.js         # Logging utilities
package.json
package-lock.json
```

### Bot Environment Variables (.env)

```env
DISCORD_TOKEN=your_discord_bot_token
DISCORD_CLIENT_ID=your_client_id
DISCORD_CLIENT_SECRET=your_client_secret
API_SECRET=generate_a_long_random_string
BOT_API_PORT=3456
WEB_PORT=3457
BASE_URL=https://your-site-url.com

# Website sync (update to your deployed site URL)
SITE_SYNC_WEBHOOK_URL=https://your-site.vercel.app/api/webhooks/discord-tier
SITE_SYNC_SECRET=generate_another_random_string
```

### Bot Start Command
```bash
npm install
npm run bot
```

---

## 2. Website Deployment (Vercel Recommended)

### Files to Upload

Upload the **`site/`** folder as a separate project:
```
site/
  ├── src/            # Source code
  ├── public/         # Static assets
  ├── package.json
  ├── next.config.js
  ├── tailwind.config.ts
  ├── tsconfig.json
  └── postcss.config.js
```

**DO NOT upload:**
- `node_modules/`
- `.next/`
- `.env.local`

### Website Environment Variables

Set these in Vercel dashboard or `.env.local`:

```env
# Supabase (from your Supabase project settings)
NEXT_PUBLIC_SUPABASE_URL=https://your-project.supabase.co
NEXT_PUBLIC_SUPABASE_ANON_KEY=your_anon_key
SUPABASE_SERVICE_ROLE_KEY=your_service_role_key

# Sync secret (must match bot's SITE_SYNC_SECRET)
SYNC_SECRET=same_as_bot_site_sync_secret
```

### Vercel Deployment Steps

1. Push `site/` folder to a GitHub repo (or upload directly)
2. Connect to Vercel
3. Set environment variables in Vercel dashboard
4. Deploy

---

## 3. Database Setup (Supabase)

Run this SQL in Supabase SQL Editor:

```sql
-- Players table
CREATE TABLE IF NOT EXISTS players (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  username TEXT NOT NULL,
  discord_id TEXT UNIQUE,
  region TEXT DEFAULT 'Middle East',
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Player tiers table
CREATE TABLE IF NOT EXISTS player_tiers (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  player_id UUID NOT NULL REFERENCES players(id) ON DELETE CASCADE,
  mode TEXT NOT NULL,
  tier TEXT NOT NULL,
  last_tested_at TIMESTAMPTZ DEFAULT NOW(),
  tester_name TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(player_id, mode)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_players_username ON players(username);
CREATE INDEX IF NOT EXISTS idx_players_discord_id ON players(discord_id);
CREATE INDEX IF NOT EXISTS idx_player_tiers_mode ON player_tiers(mode);
CREATE INDEX IF NOT EXISTS idx_player_tiers_tier ON player_tiers(tier);

-- RLS Policies
ALTER TABLE players ENABLE ROW LEVEL SECURITY;
ALTER TABLE player_tiers ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Public read players" ON players FOR SELECT USING (true);
CREATE POLICY "Service write players" ON players FOR ALL USING (auth.role() = 'service_role');

CREATE POLICY "Public read tiers" ON player_tiers FOR SELECT USING (true);
CREATE POLICY "Service write tiers" ON player_tiers FOR ALL USING (auth.role() = 'service_role');
```

---

## 4. Sync Flow

When `/result` is used:
1. Bot saves to local SQLite (backup)
2. Bot sends webhook to website
3. Website upserts player in Supabase
4. Website updates/creates tier for that mode
5. Leaderboard automatically shows new data

---

## 5. Hosting Recommendations

| Component | Recommended Host | Free Tier |
|-----------|-----------------|-----------|
| Bot | Railway, Render, Fly.io | Yes |
| Website | Vercel | Yes |
| Database | Supabase | Yes (500MB) |

---

## 6. Post-Deployment Checklist

- [ ] Bot token is valid and set
- [ ] All environment variables configured
- [ ] Supabase tables created with RLS policies
- [ ] Website SYNC_SECRET matches bot's SITE_SYNC_SECRET
- [ ] Bot SITE_SYNC_WEBHOOK_URL points to deployed website
- [ ] Test `/result` command and verify data appears on website
