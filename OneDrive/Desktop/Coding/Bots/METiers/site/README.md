# METiers MVP Website

Middle East PvP ranking site: manual skill tiers, mode-based leaderboards (Sword / Axe / Pot), and admin panel for adding/editing players.

## Setup

1. **Supabase**
   - Create a project at [supabase.com](https://supabase.com).
   - In SQL Editor, run `supabase/schema.sql` to create `players`, `player_tiers`, and RLS.

2. **Env**
   - In the repo root there is a single `.env.example` (bot + site). Copy the **Site** section into `site/.env.local`.
   - Set `NEXT_PUBLIC_SUPABASE_URL`, `NEXT_PUBLIC_SUPABASE_ANON_KEY`, `SUPABASE_SERVICE_ROLE_KEY`, and `ADMIN_PASSWORD`.
   - Optionally set `NEXT_PUBLIC_DISCORD_INVITE_URL` for the Discord CTA.

3. **Run**
   - `npm install` then `npm run dev`.
   - Open [http://localhost:3000](http://localhost:3000).

## Pages

| Path | Purpose |
|------|--------|
| `/` | Home + Sword leaderboard preview |
| `/leaderboards` | Mode selection (Sword, Axe, Pot) |
| `/mode/sword` (axe, pot) | Mode leaderboard + search & tier filter |
| `/player/:username` | Player profile + tiers per mode |
| `/rules` | Tier system & how to get ranked |
| `/discord` | Join Discord CTA |
| `/admin` | Admin login → add/edit players, assign tester, update tiers |

## Discord tier sync

Tier results from the Discord bot (`/result user tier`) can sync to the site:

1. **Add `discord_id` to players**  
   In Supabase SQL Editor (if the column is missing):  
   `alter table public.players add column discord_id text unique;`

2. **Link players to Discord**  
   In Admin → Add/Edit player, set **Discord user ID** (the Discord user’s ID, e.g. from Developer Mode) for each player you want to sync.

3. **Configure env** (in `.env.local`):
   - `BOT_API_URL` – bot API base URL (e.g. `http://localhost:3456`)
   - `BOT_API_SECRET` – same as the bot’s `API_SECRET`
   - `DISCORD_GUILD_ID` – your Discord server ID
   - `SYNC_DEFAULT_MODE` – mode to write synced tiers to (default `sword`)
   - `SYNC_SECRET` – secret for the webhook (must match bot’s `SITE_SYNC_SECRET` if using real-time sync)

4. **Sync from Discord**  
   In Admin, click **Sync from Discord** to pull all tier results from the bot for that guild. Only players with a matching `discord_id` are updated; tier must be one of the site’s tiers (e.g. HT1, LT1, …).

5. **Real-time sync (optional)**  
   In the bot’s `.env`, set `SITE_SYNC_WEBHOOK_URL` (e.g. `https://yoursite.com/api/webhooks/discord-tier`) and `SITE_SYNC_SECRET`. When someone uses `/result user tier` in Discord, the bot POSTs to the site so that player’s tier is updated immediately (if they have `discord_id` set and tier is valid).

**Note:** In Discord, set allowed tiers to match the site (e.g. `/admin tiers HT1,LT1,HT2,LT2,HT3,LT3,HT4,LT4,HT5,LT5`) so synced values are valid.

## MVP scope

- **Tiers:** HT1–HT3, MT1–MT3, LT1–LT5 (no ELO, no decimals).
- **Modes:** Sword, Axe, Pot — each with its own tier per player.
- **Testing:** Manual only (Discord → request test → tester assigns tier → added on site). Optional sync from Discord.
- **Admin:** Add player, edit tier, set tester, Discord ID for sync, Sync from Discord. No automation.
