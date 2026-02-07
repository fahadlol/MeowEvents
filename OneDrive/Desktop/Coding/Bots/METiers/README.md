# METiers – Discord Bot + PvP Ranking Site

**Discord bot** (ticket panels) and **PvP ranking site** (leaderboards, tiers). Run both from the same repo.

---

## Quick start

1. **`.env`** — Copy `.env.example` to `.env`, set `DISCORD_TOKEN` (and `DISCORD_CLIENT_ID` if needed).
2. **Invite bot** — [Discord Developer Portal](https://discord.com/developers/applications) → OAuth2 URL Generator → scopes `bot`, permissions: Manage Channels, View Channel, Send Messages, Manage Messages, Read Message History, Use Application Commands.
3. **Run** — `npm run bot`.
4. **In Discord** — `/admin results-channel #channel` (where tier results post), then `/result @user LT5` (or HT5, HT4, LT4, LT3). Use `/help` for commands.

---

## Discord bot

Discord bot that creates **ticket panels** (up to **50 per server**) and a **web dashboard** to manage them. When someone opens a ticket, the bot pings a chosen role and gives options to **close**, **add players**, and **rename** the channel.

---

## Features

- **Ticket panels** – Create up to 50 panels per server. Each panel is an embed + button in a channel. Panel footer shows **open ticket count**.
- **Role ping** – When a ticket is opened, a configurable role is mentioned.
- **Ticket actions**
  - **Close** – Opens a modal for an optional **close reason**; saves a **transcript** (with reason at top) to the log channel, then deletes the channel. Log message includes a **Reopen** button (disabled after use).
  - **Claim** / **Unclaim** – Staff can claim a ticket (shows “Claimed by @user”); **Unclaim** frees it for someone else.
  - **Add player** – Modal to enter a user ID or @mention; adds them to the channel.
  - **Rename** – Modal to set a new channel name.
- **Transcript on close** – All messages are collected and sent as a `.txt` file to the panel’s **log channel** (optional). The same message has a **Reopen** button to create a new ticket for the same user.
- **Log channel** – Optional per-panel channel where open/close/claim/add/rename events and transcripts are sent.
- **Reopen** – From the log channel, “Reopen Ticket” creates a new ticket for the same user.
- **Auto-close** – Optional “auto-close after N days” per panel. Tickets with no messages for that many days are closed automatically (transcript + log).
- **Slash commands** – `/panel create`, `/panel list`, `/panel delete` (by ID).
- **Button emoji** – Optional emoji on the Open Ticket button (dashboard).
- **Custom embed color** – Per-panel hex color (e.g. `5865f2`) for the panel embed.
- **Web dashboard** – **Edit** panels (title, description, role, category, button, log channel, auto-close, **embed color**, welcome message). Panel list shows **open ticket count** per panel. Create, edit, delete panels.

### Tier tests & tester ranks

- **Testers** – People who can record tier results. Admins add/remove testers with `/tester add` / `/tester remove`. Optionally set a **tester role** with `/admin tester-role`; anyone with that role can use `/result` to set tiers.
- **Tier results** – `/result user [tier]`: with a tier, sets that user’s tier (testers or admins only). Tiers: **LT5, HT5, HT4, LT4, LT3**. Without a tier, shows the user’s current tier (anyone). Results are posted to the server’s **results channel** (set with `/admin results-channel`).
- **Admin** – `/admin results-channel #channel` – channel where tier results are posted. `/admin tester-role [role]` – set or clear the role that counts as tester. `/admin config` – show config. `/admin clear user` – clear a user’s tier.

**Commands:** `/help` · `/tester add/remove/list` · `/result user [tier]` · `/set user tier` · `/admin results-channel`, `/admin tester-role`, `/admin config`, `/admin clear` (Manage Server for admin).

---

## Setup

1. **Clone / open the project** and install dependencies:

   ```bash
   npm install
   ```

2. **Create a Discord application** at [Discord Developer Portal](https://discord.com/developers/applications):
   - Create an application and a **Bot**.
   - Copy **Bot Token** and **Application ID**.
   - Under OAuth2, copy **Client Secret** (needed if you add OAuth later).
   - Bot → **Privileged Gateway Intents**: enable **Server Members Intent** and **Message Content Intent** (for auto-close last-message tracking).

3. **Invite the bot** to your server (OAuth2 → URL Generator):
   - Scopes: `bot`
   - Bot permissions: `Manage Channels`, `View Channel`, `Send Messages`, `Manage Messages`, `Read Message History`, `Use Application Commands` (if you add slash commands later).

4. **Environment** – Copy `.env.example` to `.env` and fill in:

   ```env
   DISCORD_TOKEN=your_bot_token
   DISCORD_CLIENT_ID=your_application_id
   DISCORD_CLIENT_SECRET=your_client_secret
   API_SECRET=some_long_random_string
   BOT_API_PORT=3456
   WEB_PORT=3457
   ```

   Optional: set `DASHBOARD_PASSWORD=your_password` to protect the dashboard with a simple login.

5. **Run** (bot + dashboard):

   ```bash
   npm start
   ```

   Or separately:

   ```bash
   npm run bot    # Discord bot + internal API
   npm run web    # Dashboard at http://localhost:3457
   npm run site   # PvP ranking site at http://localhost:3000 (see site/README)
   npm run dev:all   # Bot + ranking site together
   ```

6. Open **http://localhost:3457**, select your server, and create a panel. The bot will post the panel in the chosen channel.

### Testing sync (bot ↔ site)

- **Bot → website:** When someone uses `/result user tier` in Discord, the bot POSTs to the site webhook (`SITE_SYNC_WEBHOOK_URL`) so that player’s tier is updated on the site (if they have `discord_id` set or a matching username).
- **Website → bot:** In the site Admin panel, “Sync from Discord” calls the bot API to pull tiers from one or more servers and update the site’s player tiers.

To verify both directions:

1. Start bot + site: `npm run dev` (or `npm run bot` in one terminal and `npm run site` in another).
2. In a second terminal: `npm run test:sync`.

The script checks (1) that the site webhook accepts a POST (bot → website) and (2) that the bot API returns guilds and tiers (website → bot). Ensure root `.env` has `API_SECRET`, `SITE_SYNC_WEBHOOK_URL`, and `SITE_SYNC_SECRET`; site `.env.local` should have `SYNC_SECRET` matching the bot’s `SITE_SYNC_SECRET`.

### Sync & webhook checklist (better experience)

- **Webhook URL:** In root `.env`, `SITE_SYNC_WEBHOOK_URL` must use the **same port** the site runs on (e.g. `http://localhost:3001/api/webhooks/discord-tier` if Next.js said “trying 3001 instead”).
- **Secrets:** Root `SITE_SYNC_SECRET` and site `SYNC_SECRET` must match so the webhook is accepted.
- **After `/result`:** The bot reply now says “Site updated.” when the webhook succeeds, or “Site unreachable—use Sync from Discord in Admin” when it fails, so you know whether the site was updated.
- **Per-kit tiers:** The bot stores one tier per user **per kit** (Sword, Netherite Pot, etc.). Sync from Discord pulls all kits and updates the site per mode; the admin message shows which modes were synced.
- **Last synced:** The admin panel shows “Last synced: just now” (or “X min ago”) after a successful sync.

---

## PvP ranking site (`site/`)

Next.js app: leaderboards per mode (Sword / Axe / Pot), player profiles, rules, Discord CTA. Admin panel at `/admin` (not in nav). See **site/README.md** for Supabase setup and env.

---

## Project layout

```
METiers/
├── bot/            # Discord bot (tickets, panels)
├── web/            # Ticket dashboard (HTML/CSS/JS)
├── site/           # PvP ranking site (Next.js + Supabase)
├── data/           # metiers.db (created at runtime)
├── .env
├── .env.example
└── package.json
```

---

## Limits

- **50 panels per server** (enforced in bot and API).
- Dashboard talks to the bot over `API_SECRET`; keep it secret and use HTTPS in production.

---

## Improvements (experience & sync)

- **Webhook feedback:** After `/result`, the bot reply includes “Site updated.” or “Site unreachable—use Sync from Discord in Admin” so you know if the website was updated.
- **Per-kit storage:** Tiers are stored per kit (Sword, Netherite Pot, etc.). Sync from Discord updates the site for each mode; leaderboards show the correct kit.
- **Sync creates players:** “Sync from Discord” creates missing players (from Discord username) and links by `discord_id`, so new users appear without manual add.
- **Admin UX:** Sync result shows updated/created/skipped counts, which modes were synced, and “Last synced: X min ago.”
- **Port reminder:** `.env.example` notes that `SITE_SYNC_WEBHOOK_URL` must match the port the site runs on (e.g. 3001 if 3000 was in use).
