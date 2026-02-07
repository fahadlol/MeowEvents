const express = require('express');
const crypto = require('crypto');
const db = require('./db');
const panels = require('./panels');

const app = express();
app.use(express.json());

function auth(req, res, next) {
  const expected = process.env.API_SECRET;
  if (!expected) {
    return res.status(500).json({ error: 'API_SECRET not configured. Set it in .env' });
  }
  const key = req.headers['x-api-secret'] || req.query.secret || '';
  if (typeof key !== 'string' || key.length !== expected.length ||
      !crypto.timingSafeEqual(Buffer.from(key), Buffer.from(expected))) {
    return res.status(401).json({ error: 'Unauthorized' });
  }
  next();
}

app.use(auth);

function start(client) {
  const port = parseInt(process.env.BOT_API_PORT, 10) || 3456;

  app.get('/api/guilds', (req, res) => {
    const list = client.guilds.cache.map((g) => ({
      id: g.id,
      name: g.name,
      icon: g.iconURL({ size: 64 }),
      memberCount: g.memberCount,
    }));
    res.json(list);
  });

  app.get('/api/guilds/:id/channels', (req, res) => {
    const guild = client.guilds.cache.get(req.params.id);
    if (!guild) return res.status(404).json({ error: 'Guild not found' });
    const channels = guild.channels.cache
      .filter((c) => c.type === 0)
      .map((c) => ({ id: c.id, name: c.name, parentId: c.parentId }));
    res.json(channels);
  });

  app.get('/api/guilds/:id/roles', (req, res) => {
    const guild = client.guilds.cache.get(req.params.id);
    if (!guild) return res.status(404).json({ error: 'Guild not found' });
    const roles = guild.roles.cache
      .filter((r) => !r.managed && r.id !== guild.id)
      .map((r) => ({ id: r.id, name: r.name, color: r.hexColor }));
    res.json(roles);
  });

  app.get('/api/guilds/:id/categories', (req, res) => {
    const guild = client.guilds.cache.get(req.params.id);
    if (!guild) return res.status(404).json({ error: 'Guild not found' });
    const categories = guild.channels.cache
      .filter((c) => c.type === 4)
      .map((c) => ({ id: c.id, name: c.name }));
    res.json(categories);
  });

  app.get('/api/panels', (req, res) => {
    const guildId = req.query.guildId;
    if (!guildId) return res.status(400).json({ error: 'guildId required' });
    const list = db.getPanelsByGuild(guildId).map((p) => ({
      ...p,
      openCount: db.getOpenTicketCount(p.id),
    }));
    res.json(list);
  });

  app.post('/api/panels', async (req, res) => {
    const { guildId, channelId, roleId, categoryId, title, description, buttonLabel, buttonEmoji, customMessage, logChannelId, autoCloseDays } = req.body;
    if (!guildId || !channelId || !roleId) {
      return res.status(400).json({ error: 'guildId, channelId, roleId required' });
    }
    if (title && title.length > 256) return res.status(400).json({ error: 'Title must be 256 characters or less' });
    if (description && description.length > 4096) return res.status(400).json({ error: 'Description must be 4096 characters or less' });
    if (buttonLabel && buttonLabel.length > 80) return res.status(400).json({ error: 'Button label must be 80 characters or less' });

    const guild = client.guilds.cache.get(guildId);
    if (!guild) return res.status(404).json({ error: 'Guild not found' });

    if (db.getPanelCount(guildId) >= db.MAX_PANELS) {
      return res.status(400).json({ error: `Maximum ${db.MAX_PANELS} panels per server.` });
    }

    const channel = guild.channels.cache.get(channelId);
    if (!channel) return res.status(404).json({ error: 'Channel not found' });

    const panelData = {
      guild_id: guildId,
      channel_id: channelId,
      role_id: roleId,
      category_id: categoryId || null,
      title: title || 'Support Tickets',
      description: description || 'Click the button below to open a ticket.',
      button_label: buttonLabel || 'Open Ticket',
      button_emoji: buttonEmoji || null,
      custom_message: customMessage || null,
      log_channel_id: logChannelId || null,
      auto_close_days: autoCloseDays != null && autoCloseDays > 0 ? parseInt(autoCloseDays, 10) : null,
      embed_color: req.body.embedColor || null,
    };

    try {
      const id = db.createPanel({ ...panelData, message_id: null });
      const messageId = await panels.postPanel(channel, { ...panelData, id });
      db.updatePanelMessage(id, messageId);
      const panel = db.getPanel(id);
      res.status(201).json(panel);
    } catch (err) {
      console.error('Create panel error:', err);
      res.status(500).json({ error: err.message || 'Failed to create panel' });
    }
  });

  app.get('/api/panels/:id', (req, res) => {
    const id = parseInt(req.params.id, 10);
    const panel = db.getPanel(id);
    if (!panel) return res.status(404).json({ error: 'Panel not found' });
    res.json(panel);
  });

  app.patch('/api/panels/:id', async (req, res) => {
    const id = parseInt(req.params.id, 10);
    const panel = db.getPanel(id);
    if (!panel) return res.status(404).json({ error: 'Panel not found' });

    const { channelId, roleId, categoryId, title, description, buttonLabel, buttonEmoji, customMessage, logChannelId, autoCloseDays, embedColor } = req.body;
    const data = {};
    if (roleId !== undefined) data.role_id = roleId || null;
    if (categoryId !== undefined) data.category_id = categoryId || null;
    if (title !== undefined) data.title = title || 'Support Tickets';
    if (description !== undefined) data.description = description || 'Click the button below to open a ticket.';
    if (buttonLabel !== undefined) data.button_label = buttonLabel || 'Open Ticket';
    if (buttonEmoji !== undefined) data.button_emoji = buttonEmoji || null;
    if (customMessage !== undefined) data.custom_message = customMessage || null;
    if (logChannelId !== undefined) data.log_channel_id = logChannelId || null;
    if (autoCloseDays !== undefined) data.auto_close_days = autoCloseDays != null && autoCloseDays > 0 ? parseInt(autoCloseDays, 10) : null;
    if (embedColor !== undefined) data.embed_color = embedColor || null;

    try {
      const updated = db.updatePanel(id, data);
      await panels.updatePanelMessage(client, updated);
      res.json(updated);
    } catch (err) {
      console.error('Update panel error:', err);
      res.status(500).json({ error: err.message || 'Failed to update panel' });
    }
  });

  app.get('/api/tiers', (req, res) => {
    const guildId = req.query.guildId;
    if (!guildId) return res.status(400).json({ error: 'guildId required' });
    const rows = db.getTiersByGuild(guildId);
    const guild = client.guilds.cache.get(guildId);
    const tiers = rows.map((r) => {
      let username = null;
      if (guild) {
        const member = guild.members.cache.get(r.user_id);
        if (member?.user) username = member.user.username;
      }
      return {
        userId: r.user_id,
        username,
        tier: r.tier,
        mode: r.mode || 'sword',
        updatedAt: r.updated_at,
        updatedBy: r.updated_by,
      };
    });
    res.json({ tiers });
  });

  app.get('/api/tiers/config', (req, res) => {
    const guildId = req.query.guildId;
    if (!guildId) return res.status(400).json({ error: 'guildId required' });
    const config = db.getTesterConfig(guildId);
    res.json({
      tierNames: config?.tier_names ? config.tier_names.split(',').map((s) => s.trim()).filter(Boolean) : null,
    });
  });

  app.delete('/api/panels/:id', async (req, res) => {
    const id = parseInt(req.params.id, 10);
    const panel = db.getPanel(id);
    if (!panel) return res.status(404).json({ error: 'Panel not found' });

    try {
      const guild = client.guilds.cache.get(panel.guild_id);
      if (guild) {
        const ch = guild.channels.cache.get(panel.channel_id);
        if (ch && panel.message_id) {
          const msg = await ch.messages.fetch(panel.message_id).catch(() => null);
          if (msg) await msg.delete().catch(() => {});
        }
      }
      db.deletePanel(id);
      res.json({ ok: true });
    } catch (err) {
      console.error('Delete panel error:', err);
      res.status(500).json({ error: err.message || 'Failed to delete panel' });
    }
  });

  // ----- Ticket Types -----

  // List ticket types for a panel
  app.get('/api/panels/:id/types', (req, res) => {
    const panelId = parseInt(req.params.id, 10);
    const panel = db.getPanel(panelId);
    if (!panel) return res.status(404).json({ error: 'Panel not found' });

    const types = db.getTicketTypesByPanel(panelId).map((t) => ({
      ...t,
      questionCount: db.getQuestionCount(t.id),
    }));
    res.json(types);
  });

  // Create ticket type for a panel
  app.post('/api/panels/:id/types', async (req, res) => {
    const panelId = parseInt(req.params.id, 10);
    const panel = db.getPanel(panelId);
    if (!panel) return res.status(404).json({ error: 'Panel not found' });

    const typeCount = db.getTicketTypeCount(panelId);
    if (typeCount >= 5) {
      return res.status(400).json({ error: 'Maximum 5 ticket types per panel' });
    }

    const {
      name,
      emoji,
      buttonStyle,
      categoryId,
      staffRoleIds,
      welcomeMessage,
      namingFormat,
      autoCloseDays,
      deleteDelaySeconds,
      dmTranscript,
      allowDuplicate,
      logChannelId,
      orderIndex,
    } = req.body;

    if (!name) {
      return res.status(400).json({ error: 'name is required' });
    }

    try {
      const typeId = db.createTicketType({
        panel_id: panelId,
        name,
        emoji: emoji || null,
        button_style: buttonStyle || 'Primary',
        category_id: categoryId || panel.category_id,
        staff_role_ids: staffRoleIds || [panel.role_id],
        welcome_message: welcomeMessage || null,
        naming_format: namingFormat || '{type}-{number}',
        auto_close_days: autoCloseDays ?? null,
        delete_delay_seconds: deleteDelaySeconds ?? 5,
        dm_transcript: dmTranscript || false,
        allow_duplicate: allowDuplicate || false,
        log_channel_id: logChannelId || panel.log_channel_id,
        order_index: orderIndex ?? 0,
      });

      await panels.updatePanelMessage(client, panel);
      const ticketType = db.getTicketType(typeId);
      res.status(201).json(ticketType);
    } catch (err) {
      console.error('Create ticket type error:', err);
      res.status(500).json({ error: err.message || 'Failed to create ticket type' });
    }
  });

  // Get single ticket type
  app.get('/api/ticket-types/:id', (req, res) => {
    const id = parseInt(req.params.id, 10);
    const ticketType = db.getTicketType(id);
    if (!ticketType) return res.status(404).json({ error: 'Ticket type not found' });
    ticketType.questionCount = db.getQuestionCount(id);
    ticketType.questions = db.getQuestionsByType(id);
    res.json(ticketType);
  });

  // Update ticket type
  app.patch('/api/ticket-types/:id', async (req, res) => {
    const id = parseInt(req.params.id, 10);
    const ticketType = db.getTicketType(id);
    if (!ticketType) return res.status(404).json({ error: 'Ticket type not found' });

    const {
      name,
      emoji,
      buttonStyle,
      categoryId,
      staffRoleIds,
      welcomeMessage,
      namingFormat,
      autoCloseDays,
      deleteDelaySeconds,
      dmTranscript,
      allowDuplicate,
      logChannelId,
      orderIndex,
    } = req.body;

    const updates = {};
    if (name !== undefined) updates.name = name;
    if (emoji !== undefined) updates.emoji = emoji || null;
    if (buttonStyle !== undefined) updates.button_style = buttonStyle;
    if (categoryId !== undefined) updates.category_id = categoryId || null;
    if (staffRoleIds !== undefined) updates.staff_role_ids = staffRoleIds;
    if (welcomeMessage !== undefined) updates.welcome_message = welcomeMessage || null;
    if (namingFormat !== undefined) updates.naming_format = namingFormat;
    if (autoCloseDays !== undefined) updates.auto_close_days = autoCloseDays;
    if (deleteDelaySeconds !== undefined) updates.delete_delay_seconds = deleteDelaySeconds;
    if (dmTranscript !== undefined) updates.dm_transcript = dmTranscript;
    if (allowDuplicate !== undefined) updates.allow_duplicate = allowDuplicate;
    if (logChannelId !== undefined) updates.log_channel_id = logChannelId || null;
    if (orderIndex !== undefined) updates.order_index = orderIndex;

    try {
      const updated = db.updateTicketType(id, updates);
      const panel = db.getPanel(ticketType.panel_id);
      if (panel) await panels.updatePanelMessage(client, panel);
      res.json(updated);
    } catch (err) {
      console.error('Update ticket type error:', err);
      res.status(500).json({ error: err.message || 'Failed to update ticket type' });
    }
  });

  // Delete ticket type
  app.delete('/api/ticket-types/:id', async (req, res) => {
    const id = parseInt(req.params.id, 10);
    const ticketType = db.getTicketType(id);
    if (!ticketType) return res.status(404).json({ error: 'Ticket type not found' });

    const typeCount = db.getTicketTypeCount(ticketType.panel_id);
    if (typeCount <= 1) {
      return res.status(400).json({ error: 'Cannot delete the last ticket type. Panels must have at least one.' });
    }

    try {
      db.deleteTicketType(id);
      const panel = db.getPanel(ticketType.panel_id);
      if (panel) await panels.updatePanelMessage(client, panel);
      res.json({ ok: true });
    } catch (err) {
      console.error('Delete ticket type error:', err);
      res.status(500).json({ error: err.message || 'Failed to delete ticket type' });
    }
  });

  // ----- Ticket Questions -----

  // List questions for a ticket type
  app.get('/api/ticket-types/:id/questions', (req, res) => {
    const typeId = parseInt(req.params.id, 10);
    const ticketType = db.getTicketType(typeId);
    if (!ticketType) return res.status(404).json({ error: 'Ticket type not found' });

    const questions = db.getQuestionsByType(typeId);
    res.json(questions);
  });

  // Add question to a ticket type
  app.post('/api/ticket-types/:id/questions', (req, res) => {
    const typeId = parseInt(req.params.id, 10);
    const ticketType = db.getTicketType(typeId);
    if (!ticketType) return res.status(404).json({ error: 'Ticket type not found' });

    const questionCount = db.getQuestionCount(typeId);
    if (questionCount >= 5) {
      return res.status(400).json({ error: 'Maximum 5 questions per ticket type (Discord modal limit)' });
    }

    const { label, placeholder, style, required, minLength, maxLength, orderIndex } = req.body;
    if (!label) {
      return res.status(400).json({ error: 'label is required' });
    }

    try {
      const questionId = db.createQuestion(typeId, {
        question_label: label,
        question_placeholder: placeholder || null,
        question_style: style || 'Short',
        required: required !== false,
        min_length: minLength ?? null,
        max_length: maxLength ?? null,
        order_index: orderIndex,
      });

      const question = db.getQuestion(questionId);
      res.status(201).json(question);
    } catch (err) {
      console.error('Create question error:', err);
      res.status(500).json({ error: err.message || 'Failed to create question' });
    }
  });

  // Update question
  app.patch('/api/questions/:id', (req, res) => {
    const id = parseInt(req.params.id, 10);
    const question = db.getQuestion(id);
    if (!question) return res.status(404).json({ error: 'Question not found' });

    const { label, placeholder, style, required, minLength, maxLength, orderIndex } = req.body;

    const updates = {};
    if (label !== undefined) updates.question_label = label;
    if (placeholder !== undefined) updates.question_placeholder = placeholder || null;
    if (style !== undefined) updates.question_style = style;
    if (required !== undefined) updates.required = required;
    if (minLength !== undefined) updates.min_length = minLength;
    if (maxLength !== undefined) updates.max_length = maxLength;
    if (orderIndex !== undefined) updates.order_index = orderIndex;

    try {
      const updated = db.updateQuestion(id, updates);
      res.json(updated);
    } catch (err) {
      console.error('Update question error:', err);
      res.status(500).json({ error: err.message || 'Failed to update question' });
    }
  });

  // Delete question
  app.delete('/api/questions/:id', (req, res) => {
    const id = parseInt(req.params.id, 10);
    const question = db.getQuestion(id);
    if (!question) return res.status(404).json({ error: 'Question not found' });

    try {
      db.deleteQuestion(id);
      res.json({ ok: true });
    } catch (err) {
      console.error('Delete question error:', err);
      res.status(500).json({ error: err.message || 'Failed to delete question' });
    }
  });

  // ----- Guild Config -----

  app.get('/api/guilds/:id/config', (req, res) => {
    const guildId = req.params.id;
    const guild = client.guilds.cache.get(guildId);
    if (!guild) return res.status(404).json({ error: 'Guild not found' });

    const config = db.getGuildConfig(guildId);
    const panelCount = db.getPanelCount(guildId);

    res.json({
      guildId,
      guildName: guild.name,
      panelCount,
      maxPanels: db.MAX_PANELS,
      defaultLogChannelId: config?.default_log_channel_id || null,
    });
  });

  app.patch('/api/guilds/:id/config', (req, res) => {
    const guildId = req.params.id;
    const guild = client.guilds.cache.get(guildId);
    if (!guild) return res.status(404).json({ error: 'Guild not found' });

    const { defaultLogChannelId } = req.body;

    try {
      db.setGuildConfig(guildId, {
        default_log_channel_id: defaultLogChannelId,
      });
      const config = db.getGuildConfig(guildId);
      res.json({
        guildId,
        defaultLogChannelId: config?.default_log_channel_id || null,
      });
    } catch (err) {
      console.error('Update guild config error:', err);
      res.status(500).json({ error: err.message || 'Failed to update config' });
    }
  });

  const host = process.env.BOT_API_HOST || '127.0.0.1';
  const server = app.listen(port, host, () => {
    console.log(`METiers API listening on ${host}:${port}`);
  });
  server.on('error', (err) => {
    if (err.code === 'EADDRINUSE') {
      console.error(`Port ${port} is already in use. Close the other process using it, or set BOT_API_PORT to a different port in .env`);
      console.error('On Windows: netstat -ano | findstr :' + port + '  then  taskkill /PID <pid> /F');
      process.exit(1);
    }
    throw err;
  });
}

module.exports = { start };
