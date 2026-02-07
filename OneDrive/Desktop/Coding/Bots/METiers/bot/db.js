const Database = require('better-sqlite3');
const path = require('path');
const fs = require('fs');

const dataDir = path.join(__dirname, '..', 'data');
if (!fs.existsSync(dataDir)) fs.mkdirSync(dataDir, { recursive: true });
const db = new Database(path.join(dataDir, 'metiers.db'));

db.exec(`
  CREATE TABLE IF NOT EXISTS panels (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    guild_id TEXT NOT NULL,
    channel_id TEXT NOT NULL,
    message_id TEXT,
    role_id TEXT NOT NULL,
    category_id TEXT,
    title TEXT DEFAULT 'Support Tickets',
    description TEXT DEFAULT 'Click the button below to open a ticket.',
    button_label TEXT DEFAULT 'Open Ticket',
    button_emoji TEXT,
    custom_message TEXT,
    created_at INTEGER DEFAULT (strftime('%s', 'now'))
  );

  CREATE TABLE IF NOT EXISTS tickets (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    panel_id INTEGER NOT NULL,
    channel_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    status TEXT DEFAULT 'open',
    number INTEGER NOT NULL,
    created_at INTEGER DEFAULT (strftime('%s', 'now')),
    closed_at INTEGER,
    FOREIGN KEY (panel_id) REFERENCES panels(id)
  );

  CREATE INDEX IF NOT EXISTS idx_panels_guild ON panels(guild_id);
  CREATE INDEX IF NOT EXISTS idx_tickets_panel ON tickets(panel_id);
  CREATE INDEX IF NOT EXISTS idx_tickets_channel ON tickets(channel_id);

  CREATE TABLE IF NOT EXISTS guild_tester_config (
    guild_id TEXT PRIMARY KEY,
    tester_role_id TEXT,
    tier_names TEXT
  );

  CREATE TABLE IF NOT EXISTS guild_testers (
    guild_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    PRIMARY KEY (guild_id, user_id)
  );

  CREATE TABLE IF NOT EXISTS user_tiers (
    guild_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    tier TEXT NOT NULL,
    updated_at INTEGER DEFAULT (strftime('%s', 'now')),
    updated_by TEXT NOT NULL,
    PRIMARY KEY (guild_id, user_id)
  );

  CREATE TABLE IF NOT EXISTS user_tier_modes (
    guild_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    mode TEXT NOT NULL DEFAULT 'sword',
    tier TEXT NOT NULL,
    updated_at INTEGER DEFAULT (strftime('%s', 'now')),
    updated_by TEXT NOT NULL,
    PRIMARY KEY (guild_id, user_id, mode)
  );

  CREATE INDEX IF NOT EXISTS idx_user_tiers_guild ON user_tiers(guild_id);
  CREATE INDEX IF NOT EXISTS idx_user_tier_modes_guild ON user_tier_modes(guild_id);

  -- Migrate legacy user_tiers into user_tier_modes (one-time; OR IGNORE avoids duplicates)
  INSERT OR IGNORE INTO user_tier_modes (guild_id, user_id, mode, tier, updated_at, updated_by)
  SELECT guild_id, user_id, 'sword', tier, updated_at, updated_by FROM user_tiers;

  -- Ticket types (buttons) for each panel
  CREATE TABLE IF NOT EXISTS ticket_types (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    panel_id INTEGER NOT NULL,
    name TEXT NOT NULL,
    emoji TEXT,
    button_style TEXT DEFAULT 'Primary',
    category_id TEXT,
    staff_role_ids TEXT,
    welcome_message TEXT,
    naming_format TEXT DEFAULT '{type}-{number}',
    auto_close_days INTEGER,
    delete_delay_seconds INTEGER DEFAULT 5,
    dm_transcript INTEGER DEFAULT 0,
    allow_duplicate INTEGER DEFAULT 0,
    log_channel_id TEXT,
    order_index INTEGER DEFAULT 0,
    created_at INTEGER DEFAULT (strftime('%s', 'now')),
    FOREIGN KEY (panel_id) REFERENCES panels(id) ON DELETE CASCADE
  );

  -- Form questions for each ticket type
  CREATE TABLE IF NOT EXISTS ticket_questions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ticket_type_id INTEGER NOT NULL,
    question_label TEXT NOT NULL,
    question_placeholder TEXT,
    question_style TEXT DEFAULT 'Short',
    required INTEGER DEFAULT 1,
    min_length INTEGER,
    max_length INTEGER,
    order_index INTEGER DEFAULT 0,
    FOREIGN KEY (ticket_type_id) REFERENCES ticket_types(id) ON DELETE CASCADE
  );

  -- Form responses for tickets
  CREATE TABLE IF NOT EXISTS ticket_responses (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ticket_id INTEGER NOT NULL,
    question_id INTEGER NOT NULL,
    response TEXT,
    FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE
  );

  CREATE INDEX IF NOT EXISTS idx_ticket_types_panel ON ticket_types(panel_id);
  CREATE INDEX IF NOT EXISTS idx_ticket_questions_type ON ticket_questions(ticket_type_id);
  CREATE INDEX IF NOT EXISTS idx_ticket_responses_ticket ON ticket_responses(ticket_id);

  -- Guild configuration
  CREATE TABLE IF NOT EXISTS guild_config (
    guild_id TEXT PRIMARY KEY,
    default_log_channel_id TEXT
  );

  -- Guild kits (available kits/modes for tier testing)
  CREATE TABLE IF NOT EXISTS guild_kits (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    guild_id TEXT NOT NULL,
    kit_name TEXT NOT NULL,
    kit_label TEXT NOT NULL,
    emoji TEXT,
    order_index INTEGER DEFAULT 0,
    UNIQUE(guild_id, kit_name)
  );
`);

// Migrations: add new columns if missing
function migrate() {
  const panelCols = db.prepare("SELECT name FROM pragma_table_info('panels')").all().map((r) => r.name);
  if (!panelCols.includes('log_channel_id')) {
    db.exec('ALTER TABLE panels ADD COLUMN log_channel_id TEXT');
  }
  if (!panelCols.includes('auto_close_days')) {
    db.exec('ALTER TABLE panels ADD COLUMN auto_close_days INTEGER');
  }
  const ticketCols = db.prepare("SELECT name FROM pragma_table_info('tickets')").all().map((r) => r.name);
  if (!ticketCols.includes('claimed_by')) {
    db.exec('ALTER TABLE tickets ADD COLUMN claimed_by TEXT');
  }
  if (!ticketCols.includes('claimed_at')) {
    db.exec('ALTER TABLE tickets ADD COLUMN claimed_at INTEGER');
  }
  if (!ticketCols.includes('last_message_at')) {
    db.exec('ALTER TABLE tickets ADD COLUMN last_message_at INTEGER');
  }
  if (!panelCols.includes('embed_color')) {
    db.exec('ALTER TABLE panels ADD COLUMN embed_color TEXT');
  }
  if (!panelCols.includes('disabled')) {
    db.exec('ALTER TABLE panels ADD COLUMN disabled INTEGER DEFAULT 0');
  }
  const configCols = db.prepare("SELECT name FROM pragma_table_info('guild_tester_config')").all().map((r) => r.name);
  if (!configCols.includes('results_channel_id')) {
    db.exec('ALTER TABLE guild_tester_config ADD COLUMN results_channel_id TEXT');
  }

  // Add ticket_type_id to tickets table
  const ticketCols2 = db.prepare("SELECT name FROM pragma_table_info('tickets')").all().map((r) => r.name);
  if (!ticketCols2.includes('ticket_type_id')) {
    db.exec('ALTER TABLE tickets ADD COLUMN ticket_type_id INTEGER');
  }

  // Migrate existing panels without ticket types - create default type
  const panelsWithoutTypes = db.prepare(`
    SELECT p.* FROM panels p
    WHERE NOT EXISTS (SELECT 1 FROM ticket_types tt WHERE tt.panel_id = p.id)
  `).all();

  for (const panel of panelsWithoutTypes) {
    const stmt = db.prepare(`
      INSERT INTO ticket_types (panel_id, name, emoji, button_style, category_id, staff_role_ids, welcome_message, auto_close_days, log_channel_id, order_index)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    `);
    const typeResult = stmt.run(
      panel.id,
      panel.button_label || 'Open Ticket',
      panel.button_emoji || null,
      'Primary',
      panel.category_id || null,
      JSON.stringify([panel.role_id]),
      panel.custom_message || null,
      panel.auto_close_days || null,
      panel.log_channel_id || null,
      0
    );
    // Update existing open tickets to reference this new type
    db.prepare(`
      UPDATE tickets SET ticket_type_id = ? WHERE panel_id = ? AND ticket_type_id IS NULL
    `).run(typeResult.lastInsertRowid, panel.id);
  }
}
migrate();

// ----- Tester / tier result -----
function getTesterConfig(guildId) {
  return db.prepare('SELECT * FROM guild_tester_config WHERE guild_id = ?').get(guildId);
}

function setTesterConfig(guildId, data) {
  const existing = getTesterConfig(guildId);
  if (existing) {
    if (data.tester_role_id !== undefined) db.prepare('UPDATE guild_tester_config SET tester_role_id = ? WHERE guild_id = ?').run(data.tester_role_id || null, guildId);
    if (data.tier_names !== undefined) db.prepare('UPDATE guild_tester_config SET tier_names = ? WHERE guild_id = ?').run(data.tier_names || null, guildId);
    if (data.results_channel_id !== undefined) db.prepare('UPDATE guild_tester_config SET results_channel_id = ? WHERE guild_id = ?').run(data.results_channel_id || null, guildId);
  } else {
    db.prepare('INSERT INTO guild_tester_config (guild_id, tester_role_id, tier_names, results_channel_id) VALUES (?, ?, ?, ?)').run(
      guildId,
      data.tester_role_id ?? null,
      data.tier_names ?? null,
      data.results_channel_id ?? null
    );
  }
  return getTesterConfig(guildId);
}

function addTester(guildId, userId) {
  return db.prepare('INSERT OR IGNORE INTO guild_testers (guild_id, user_id) VALUES (?, ?)').run(guildId, userId);
}

function removeTester(guildId, userId) {
  return db.prepare('DELETE FROM guild_testers WHERE guild_id = ? AND user_id = ?').run(guildId, userId);
}

function isTester(guildId, userId, member) {
  const config = getTesterConfig(guildId);
  if (config?.tester_role_id && member?.roles?.cache?.has(config.tester_role_id)) return true;
  const row = db.prepare('SELECT 1 FROM guild_testers WHERE guild_id = ? AND user_id = ?').get(guildId, userId);
  return !!row;
}

function getTesters(guildId) {
  return db.prepare('SELECT user_id FROM guild_testers WHERE guild_id = ?').all(guildId);
}

function setUserTier(guildId, userId, tier, updatedBy, mode = 'sword') {
  db.prepare(`
    INSERT INTO user_tier_modes (guild_id, user_id, mode, tier, updated_at, updated_by)
    VALUES (?, ?, ?, ?, strftime('%s', 'now'), ?)
    ON CONFLICT(guild_id, user_id, mode) DO UPDATE SET tier = excluded.tier, updated_at = strftime('%s', 'now'), updated_by = excluded.updated_by
  `).run(guildId, userId, mode, tier, updatedBy);
}

function getUserTier(guildId, userId, mode = null) {
  if (mode) {
    return db.prepare('SELECT * FROM user_tier_modes WHERE guild_id = ? AND user_id = ? AND mode = ?').get(guildId, userId, mode);
  }
  return db.prepare('SELECT * FROM user_tier_modes WHERE guild_id = ? AND user_id = ? ORDER BY updated_at DESC LIMIT 1').get(guildId, userId);
}

function deleteUserTier(guildId, userId) {
  return db.prepare('DELETE FROM user_tier_modes WHERE guild_id = ? AND user_id = ?').run(guildId, userId);
}

function hasOpenTicketForPanel(panelId, userId) {
  const row = db.prepare('SELECT 1 FROM tickets WHERE panel_id = ? AND user_id = ? AND status = ?').get(panelId, userId, 'open');
  return !!row;
}

function getTierNames(guildId) {
  const config = getTesterConfig(guildId);
  if (!config?.tier_names) return null;
  return config.tier_names.split(',').map((s) => s.trim()).filter(Boolean);
}

function getTiersByGuild(guildId) {
  return db.prepare('SELECT * FROM user_tier_modes WHERE guild_id = ? ORDER BY updated_at DESC').all(guildId);
}

const MAX_PANELS = 50;
const MAX_TICKETS_PER_PANEL = 50;

function getPanelCount(guildId) {
  const row = db.prepare('SELECT COUNT(*) as c FROM panels WHERE guild_id = ?').get(guildId);
  return row.c;
}

function createPanel(data) {
  if (getPanelCount(data.guild_id) >= MAX_PANELS) {
    throw new Error(`Maximum ${MAX_PANELS} panels per server.`);
  }
  const stmt = db.prepare(`
    INSERT INTO panels (guild_id, channel_id, message_id, role_id, category_id, title, description, button_label, button_emoji, custom_message, log_channel_id, auto_close_days, embed_color)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `);
  const result = stmt.run(
    data.guild_id,
    data.channel_id,
    data.message_id || null,
    data.role_id,
    data.category_id || null,
    data.title || 'Support Tickets',
    data.description || 'Click the button below to open a ticket.',
    data.button_label || 'Open Ticket',
    data.button_emoji || null,
    data.custom_message || null,
    data.log_channel_id || null,
    data.auto_close_days ?? null,
    data.embed_color || null
  );
  return result.lastInsertRowid;
}

function updatePanel(id, data) {
  const panel = getPanel(id);
  if (!panel) return null;
  const updates = [];
  const values = [];
  const fields = ['channel_id', 'message_id', 'role_id', 'category_id', 'title', 'description', 'button_label', 'button_emoji', 'custom_message', 'log_channel_id', 'auto_close_days', 'embed_color', 'disabled'];
  for (const key of fields) {
    if (data[key] !== undefined) {
      updates.push(`${key} = ?`);
      values.push(data[key] === null || data[key] === '' ? null : data[key]);
    }
  }
  if (updates.length === 0) return panel;
  values.push(id);
  db.prepare(`UPDATE panels SET ${updates.join(', ')} WHERE id = ?`).run(...values);
  return getPanel(id);
}

function updatePanelMessage(id, messageId) {
  db.prepare('UPDATE panels SET message_id = ? WHERE id = ?').run(messageId, id);
}

function getPanel(id) {
  return db.prepare('SELECT * FROM panels WHERE id = ?').get(id);
}

function getPanelsByGuild(guildId) {
  return db.prepare('SELECT * FROM panels WHERE guild_id = ? ORDER BY id').all(guildId);
}

function getPanelByTitle(guildId, title) {
  const list = db.prepare('SELECT * FROM panels WHERE guild_id = ? AND LOWER(TRIM(title)) = LOWER(TRIM(?))').all(guildId, title);
  return list[0] || null;
}

function setPanelDisabled(panelId, disabled) {
  db.prepare('UPDATE panels SET disabled = ? WHERE id = ?').run(disabled ? 1 : 0, panelId);
  return getPanel(panelId);
}

function deletePanel(id) {
  const run = db.transaction(() => {
    db.prepare('DELETE FROM ticket_responses WHERE ticket_id IN (SELECT id FROM tickets WHERE panel_id = ?)').run(id);
    db.prepare('DELETE FROM tickets WHERE panel_id = ?').run(id);
    db.prepare('DELETE FROM ticket_types WHERE panel_id = ?').run(id);
    return db.prepare('DELETE FROM panels WHERE id = ?').run(id);
  });
  return run();
}

function getNextTicketNumber(panelId) {
  const row = db.prepare('SELECT COALESCE(MAX(number), 0) + 1 as next FROM tickets WHERE panel_id = ?').get(panelId);
  return row.next;
}

function getOpenTicketCount(panelId) {
  const row = db.prepare('SELECT COUNT(*) as c FROM tickets WHERE panel_id = ? AND status = ?').get(panelId, 'open');
  return row.c;
}

function createTicket(panelId, channelId, userId) {
  const now = Math.floor(Date.now() / 1000);
  const result = db.prepare(`
    INSERT INTO tickets (panel_id, channel_id, user_id, number, last_message_at)
    VALUES (?, ?, ?, (SELECT COALESCE(MAX(number), 0) + 1 FROM tickets WHERE panel_id = ?), ?)
  `).run(panelId, channelId, userId, panelId, now);
  const row = db.prepare('SELECT number FROM tickets WHERE id = ?').get(result.lastInsertRowid);
  return { id: result.lastInsertRowid, number: row.number };
}

function getTicketByChannel(channelId) {
  return db.prepare("SELECT * FROM tickets WHERE channel_id = ? AND status IN ('open', 'closing')").get(channelId);
}

function getTicket(id) {
  return db.prepare('SELECT * FROM tickets WHERE id = ?').get(id);
}

function closeTicket(channelId) {
  return db.prepare(`
    UPDATE tickets SET status = 'closed', closed_at = strftime('%s', 'now') WHERE channel_id = ? AND status IN ('open', 'closing')
  `).run(channelId);
}

function setTicketClaimed(channelId, userId) {
  return db.prepare(`
    UPDATE tickets SET claimed_by = ?, claimed_at = strftime('%s', 'now') WHERE channel_id = ?
  `).run(userId, channelId);
}

function clearTicketClaimed(channelId) {
  return db.prepare(`
    UPDATE tickets SET claimed_by = NULL, claimed_at = NULL WHERE channel_id = ?
  `).run(channelId);
}

function updateTicketLastMessage(channelId) {
  return db.prepare(`
    UPDATE tickets SET last_message_at = strftime('%s', 'now') WHERE channel_id = ? AND status = 'open'
  `).run(channelId);
}

function getTicketsForAutoClose() {
  // Check ticket type auto_close_days first, then fall back to panel
  return db.prepare(`
    SELECT t.*, COALESCE(tt.auto_close_days, p.auto_close_days) as effective_auto_close FROM tickets t
    JOIN panels p ON p.id = t.panel_id
    LEFT JOIN ticket_types tt ON tt.id = t.ticket_type_id
    WHERE t.status = 'open'
    AND COALESCE(tt.auto_close_days, p.auto_close_days) IS NOT NULL
    AND COALESCE(tt.auto_close_days, p.auto_close_days) > 0
    AND (t.last_message_at IS NULL OR (strftime('%s', 'now') - t.last_message_at) > (COALESCE(tt.auto_close_days, p.auto_close_days) * 86400))
  `).all();
}

// ----- Ticket Types -----
function createTicketType(data) {
  const stmt = db.prepare(`
    INSERT INTO ticket_types (panel_id, name, emoji, button_style, category_id, staff_role_ids, welcome_message, naming_format, auto_close_days, delete_delay_seconds, dm_transcript, allow_duplicate, log_channel_id, order_index)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `);
  const result = stmt.run(
    data.panel_id,
    data.name,
    data.emoji || null,
    data.button_style || 'Primary',
    data.category_id || null,
    data.staff_role_ids ? JSON.stringify(data.staff_role_ids) : null,
    data.welcome_message || null,
    data.naming_format || '{type}-{number}',
    data.auto_close_days ?? null,
    data.delete_delay_seconds ?? 5,
    data.dm_transcript ? 1 : 0,
    data.allow_duplicate ? 1 : 0,
    data.log_channel_id || null,
    data.order_index ?? 0
  );
  return result.lastInsertRowid;
}

function updateTicketType(id, data) {
  const ticketType = getTicketType(id);
  if (!ticketType) return null;
  const updates = [];
  const values = [];
  const fields = ['name', 'emoji', 'button_style', 'category_id', 'welcome_message', 'naming_format', 'auto_close_days', 'delete_delay_seconds', 'dm_transcript', 'allow_duplicate', 'log_channel_id', 'order_index'];
  for (const key of fields) {
    if (data[key] !== undefined) {
      updates.push(`${key} = ?`);
      if (key === 'dm_transcript' || key === 'allow_duplicate') {
        values.push(data[key] ? 1 : 0);
      } else {
        values.push(data[key] === null || data[key] === '' ? null : data[key]);
      }
    }
  }
  if (data.staff_role_ids !== undefined) {
    updates.push('staff_role_ids = ?');
    values.push(data.staff_role_ids ? JSON.stringify(data.staff_role_ids) : null);
  }
  if (updates.length === 0) return ticketType;
  values.push(id);
  db.prepare(`UPDATE ticket_types SET ${updates.join(', ')} WHERE id = ?`).run(...values);
  return getTicketType(id);
}

function deleteTicketType(id) {
  return db.prepare('DELETE FROM ticket_types WHERE id = ?').run(id);
}

function getTicketType(id) {
  const row = db.prepare('SELECT * FROM ticket_types WHERE id = ?').get(id);
  if (row && row.staff_role_ids) {
    try {
      row.staff_role_ids = JSON.parse(row.staff_role_ids);
    } catch {
      row.staff_role_ids = [];
    }
  }
  return row;
}

function getTicketTypesByPanel(panelId) {
  const rows = db.prepare('SELECT * FROM ticket_types WHERE panel_id = ? ORDER BY order_index, id').all(panelId);
  return rows.map(row => {
    if (row.staff_role_ids) {
      try {
        row.staff_role_ids = JSON.parse(row.staff_role_ids);
      } catch {
        row.staff_role_ids = [];
      }
    }
    return row;
  });
}

function getTicketTypeCount(panelId) {
  const row = db.prepare('SELECT COUNT(*) as c FROM ticket_types WHERE panel_id = ?').get(panelId);
  return row.c;
}

// ----- Ticket Questions -----
function createQuestion(typeId, data) {
  const maxOrder = db.prepare('SELECT COALESCE(MAX(order_index), -1) + 1 as next FROM ticket_questions WHERE ticket_type_id = ?').get(typeId);
  const stmt = db.prepare(`
    INSERT INTO ticket_questions (ticket_type_id, question_label, question_placeholder, question_style, required, min_length, max_length, order_index)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
  `);
  const result = stmt.run(
    typeId,
    data.question_label,
    data.question_placeholder || null,
    data.question_style || 'Short',
    data.required !== false ? 1 : 0,
    data.min_length ?? null,
    data.max_length ?? null,
    data.order_index ?? maxOrder.next
  );
  return result.lastInsertRowid;
}

function updateQuestion(id, data) {
  const question = getQuestion(id);
  if (!question) return null;
  const updates = [];
  const values = [];
  const fields = ['question_label', 'question_placeholder', 'question_style', 'min_length', 'max_length', 'order_index'];
  for (const key of fields) {
    if (data[key] !== undefined) {
      updates.push(`${key} = ?`);
      values.push(data[key] === null || data[key] === '' ? null : data[key]);
    }
  }
  if (data.required !== undefined) {
    updates.push('required = ?');
    values.push(data.required ? 1 : 0);
  }
  if (updates.length === 0) return question;
  values.push(id);
  db.prepare(`UPDATE ticket_questions SET ${updates.join(', ')} WHERE id = ?`).run(...values);
  return getQuestion(id);
}

function deleteQuestion(id) {
  return db.prepare('DELETE FROM ticket_questions WHERE id = ?').run(id);
}

function getQuestion(id) {
  return db.prepare('SELECT * FROM ticket_questions WHERE id = ?').get(id);
}

function getQuestionsByType(typeId) {
  return db.prepare('SELECT * FROM ticket_questions WHERE ticket_type_id = ? ORDER BY order_index, id').all(typeId);
}

function getQuestionCount(typeId) {
  const row = db.prepare('SELECT COUNT(*) as c FROM ticket_questions WHERE ticket_type_id = ?').get(typeId);
  return row.c;
}

// ----- Ticket Responses -----
function saveTicketResponses(ticketId, responses) {
  const stmt = db.prepare('INSERT INTO ticket_responses (ticket_id, question_id, response) VALUES (?, ?, ?)');
  const insertMany = db.transaction((items) => {
    for (const item of items) {
      stmt.run(ticketId, item.question_id, item.response);
    }
  });
  insertMany(responses);
}

function getTicketResponses(ticketId) {
  return db.prepare(`
    SELECT tr.*, tq.question_label, tq.question_style
    FROM ticket_responses tr
    JOIN ticket_questions tq ON tq.id = tr.question_id
    WHERE tr.ticket_id = ?
    ORDER BY tq.order_index, tq.id
  `).all(ticketId);
}

// ----- Duplicate Prevention -----
function hasOpenTicketForType(typeId, userId) {
  const row = db.prepare('SELECT 1 FROM tickets WHERE ticket_type_id = ? AND user_id = ? AND status = ?').get(typeId, userId, 'open');
  return !!row;
}

// ----- Guild Config -----
function getGuildConfig(guildId) {
  return db.prepare('SELECT * FROM guild_config WHERE guild_id = ?').get(guildId);
}

function setGuildConfig(guildId, data) {
  const existing = getGuildConfig(guildId);
  if (existing) {
    const updates = [];
    const values = [];
    if (data.default_log_channel_id !== undefined) {
      updates.push('default_log_channel_id = ?');
      values.push(data.default_log_channel_id || null);
    }
    if (updates.length > 0) {
      values.push(guildId);
      db.prepare(`UPDATE guild_config SET ${updates.join(', ')} WHERE guild_id = ?`).run(...values);
    }
  } else {
    db.prepare('INSERT INTO guild_config (guild_id, default_log_channel_id) VALUES (?, ?)').run(
      guildId,
      data.default_log_channel_id || null
    );
  }
  return getGuildConfig(guildId);
}

// Update createTicket to support ticket_type_id
function createTicketWithType(panelId, channelId, userId, ticketTypeId) {
  const now = Math.floor(Date.now() / 1000);
  const result = db.prepare(`
    INSERT INTO tickets (panel_id, channel_id, user_id, number, last_message_at, ticket_type_id)
    VALUES (?, ?, ?, (SELECT COALESCE(MAX(number), 0) + 1 FROM tickets WHERE panel_id = ?), ?, ?)
  `).run(panelId, channelId, userId, panelId, now, ticketTypeId || null);
  const row = db.prepare('SELECT number FROM tickets WHERE id = ?').get(result.lastInsertRowid);
  return { id: result.lastInsertRowid, number: row.number };
}

// Update ticket status (for closing flow)
function setTicketStatus(channelId, status) {
  return db.prepare('UPDATE tickets SET status = ? WHERE channel_id = ?').run(status, channelId);
}

// ----- Guild Kits -----
function addGuildKit(guildId, kitName, kitLabel, emoji = null) {
  const maxOrder = db.prepare('SELECT COALESCE(MAX(order_index), -1) + 1 as next FROM guild_kits WHERE guild_id = ?').get(guildId);
  return db.prepare(`
    INSERT OR REPLACE INTO guild_kits (guild_id, kit_name, kit_label, emoji, order_index)
    VALUES (?, ?, ?, ?, ?)
  `).run(guildId, kitName.toLowerCase(), kitLabel, emoji, maxOrder.next);
}

function removeGuildKit(guildId, kitName) {
  return db.prepare('DELETE FROM guild_kits WHERE guild_id = ? AND kit_name = ?').run(guildId, kitName.toLowerCase());
}

function getGuildKits(guildId) {
  return db.prepare('SELECT * FROM guild_kits WHERE guild_id = ? ORDER BY order_index, id').all(guildId);
}

function clearGuildKits(guildId) {
  return db.prepare('DELETE FROM guild_kits WHERE guild_id = ?').run(guildId);
}

function cleanupOrphanedClosingTickets() {
  return db.prepare("UPDATE tickets SET status = 'open' WHERE status = 'closing'").run();
}

module.exports = {
  db,
  MAX_PANELS,
  MAX_TICKETS_PER_PANEL,
  getPanelCount,
  getOpenTicketCount,
  createPanel,
  updatePanel,
  updatePanelMessage,
  getPanel,
  getPanelsByGuild,
  getPanelByTitle,
  setPanelDisabled,
  deletePanel,
  getNextTicketNumber,
  createTicket,
  createTicketWithType,
  getTicketByChannel,
  getTicket,
  closeTicket,
  setTicketClaimed,
  clearTicketClaimed,
  updateTicketLastMessage,
  getTicketsForAutoClose,
  setTicketStatus,
  getTesterConfig,
  setTesterConfig,
  addTester,
  removeTester,
  isTester,
  getTesters,
  setUserTier,
  getUserTier,
  deleteUserTier,
  getTierNames,
  getTiersByGuild,
  hasOpenTicketForPanel,
  // Ticket types
  createTicketType,
  updateTicketType,
  deleteTicketType,
  getTicketType,
  getTicketTypesByPanel,
  getTicketTypeCount,
  // Questions
  createQuestion,
  updateQuestion,
  deleteQuestion,
  getQuestion,
  getQuestionsByType,
  getQuestionCount,
  // Responses
  saveTicketResponses,
  getTicketResponses,
  // Duplicate prevention
  hasOpenTicketForType,
  // Guild config
  getGuildConfig,
  setGuildConfig,
  // Guild kits
  addGuildKit,
  removeGuildKit,
  getGuildKits,
  clearGuildKits,
  cleanupOrphanedClosingTickets,
};
