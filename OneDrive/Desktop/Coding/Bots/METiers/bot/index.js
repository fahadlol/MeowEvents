require('dotenv').config({ path: require('path').resolve(__dirname, '../.env') });
const {
  Client,
  GatewayIntentBits,
  Partials,
  EmbedBuilder,
  ActionRowBuilder,
  ButtonBuilder,
  ButtonStyle,
  ModalBuilder,
  TextInputBuilder,
  TextInputStyle,
  PermissionFlagsBits,
  AttachmentBuilder,
  MessageFlagsBitField,
} = require('discord.js');
const db = require('./db');
const panels = require('./panels');
const transcript = require('./transcript');
const logs = require('./logs');

// Map to track pending ticket deletions (for cancel close)
const pendingDeletions = new Map();

const client = new Client({
  intents: [
    GatewayIntentBits.Guilds,
    GatewayIntentBits.GuildMessages,
    GatewayIntentBits.DirectMessages,
    GatewayIntentBits.GuildMembers,
    GatewayIntentBits.MessageContent,
  ],
  partials: [Partials.Channel, Partials.Message],
  rest: {
    timeout: 30_000, // 30s (default 10s) – helps on slow or restricted networks
  },
});

client.once('clientReady', () => {
  console.log(`METiers logged in as ${client.user.username}`);

  // Clean up tickets stuck in 'closing' from previous crash/restart
  const cleaned = db.cleanupOrphanedClosingTickets();
  if (cleaned.changes > 0) console.log(`Cleaned ${cleaned.changes} orphaned closing ticket(s).`);

  require('./api').start(client);
  const panelCommands = require('./commands').getCommands();
  const testerCommands = require('./tester').getCommands();
  const rankCommands = require('./rank').getCommands();
  client.application.commands.set([...panelCommands, ...testerCommands, ...rankCommands]).catch((err) => console.error('Slash command set error:', err));
  require('./commands').register(client);
  require('./tester').register(client);
  require('./rank').register(client);
  startAutoClose(client);
});

// ----- Close ticket helper: transcript, log, delete, update panel -----
async function closeTicketWithTranscript(client, ticket, closedBy) {
  const panel = db.getPanel(ticket.panel_id);
  const ticketType = ticket.ticket_type_id ? db.getTicketType(ticket.ticket_type_id) : null;
  const guild = client.guilds.cache.get(panel.guild_id);
  const channel = guild?.channels.cache.get(ticket.channel_id);
  const typeName = ticketType?.name || 'Ticket';

  if (channel) {
    try {
      // Generate HTML transcript
      const htmlTranscript = await transcript.buildHtmlTranscript(channel, ticket);
      const buf = Buffer.from(htmlTranscript, 'utf8');
      const file = new AttachmentBuilder(buf, { name: `ticket-${ticket.number}-transcript.html` });
      const embed = new EmbedBuilder()
        .setColor(0xed4245)
        .setTitle(`${typeName} - Ticket #${ticket.number} closed`)
        .setDescription(`Closed by: ${closedBy}`)
        .setTimestamp();
      const reopenRow = panels.buildReopenRow(ticket.id);
      const logChannelId = ticketType?.log_channel_id || panel.log_channel_id;
      if (logChannelId && guild) {
        const logCh = guild.channels.cache.get(logChannelId);
        if (logCh) await logCh.send({ embeds: [embed], files: [file], components: [reopenRow] }).catch(() => {});
      }

      // DM user if enabled
      if (ticketType?.dm_transcript) {
        try {
          const user = await client.users.fetch(ticket.user_id).catch(() => null);
          if (user) {
            const dmBuf = Buffer.from(htmlTranscript, 'utf8');
            const dmFile = new AttachmentBuilder(dmBuf, { name: `ticket-${ticket.number}-transcript.html` });
            const dmEmbed = new EmbedBuilder()
              .setColor(0xed4245)
              .setTitle(`Your ${typeName} ticket has been closed`)
              .setDescription(`Ticket #${ticket.number} in **${guild.name}** has been closed.\nClosed by: ${closedBy}`)
              .setTimestamp();
            await user.send({ embeds: [dmEmbed], files: [dmFile] }).catch(() => {});
          }
        } catch (e) {
          console.error('DM transcript error:', e);
        }
      }
    } catch (e) {
      console.error('Transcript error:', e);
    }
    await channel.delete().catch(() => {});
  }

  db.closeTicket(ticket.channel_id);
  await panels.updatePanelMessage(client, panel);
}

// ----- Button: Open Ticket -----
client.on('interactionCreate', async (interaction) => {
  if (!interaction.isButton()) return;

  const safeReply = async (content, opts = {}) => {
    try {
      if (interaction.replied || interaction.deferred) await interaction.editReply(typeof content === 'string' ? { content, ...opts } : content).catch(() => {});
      else await interaction.reply(typeof content === 'string' ? { content, flags: MessageFlagsBitField.Flags.Ephemeral, ...opts } : { ...content, flags: MessageFlagsBitField.Flags.Ephemeral }).catch(() => {});
    } catch (_) {}
  };

  try {
  if (interaction.customId.startsWith('ticket_open:')) {
    const panelId = parseInt(interaction.customId.split(':')[1], 10);
    const panel = db.getPanel(panelId);
    if (!panel) return interaction.reply({ content: 'This panel no longer exists.', flags: MessageFlagsBitField.Flags.Ephemeral });
    if (panel.disabled) return interaction.reply({ content: 'Tickets are temporarily disabled for this panel.', flags: MessageFlagsBitField.Flags.Ephemeral });
    if (db.getOpenTicketCount(panelId) >= db.MAX_TICKETS_PER_PANEL) {
      return interaction.reply({ content: `This panel has reached the maximum of ${db.MAX_TICKETS_PER_PANEL} open tickets.`, flags: MessageFlagsBitField.Flags.Ephemeral });
    }
    if (db.hasOpenTicketForPanel(panelId, interaction.user.id)) {
      return interaction.reply({ content: 'You already have an open ticket. Close it first to open another.', flags: MessageFlagsBitField.Flags.Ephemeral });
    }

    await interaction.deferReply({ flags: MessageFlagsBitField.Flags.Ephemeral });
    const guild = interaction.guild;
    const categoryId = panel.category_id || null;
    const ticketNum = db.getNextTicketNumber(panelId);
    const channelName = `ticket-${ticketNum}`;

    const overwrites = [
      { id: guild.id, type: 0, deny: [PermissionFlagsBits.ViewChannel] },
      { id: interaction.user.id, type: 1, allow: [PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages, PermissionFlagsBits.ReadMessageHistory] },
    ];
    if (panel.role_id) {
      overwrites.push({
        id: panel.role_id,
        type: 0,
        allow: [PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages, PermissionFlagsBits.ReadMessageHistory],
      });
    }
    const ch = await guild.channels.create({
      name: channelName,
      parent: categoryId,
      permissionOverwrites: overwrites,
    });

    const { id: ticketId, number } = db.createTicket(panelId, ch.id, interaction.user.id);

    const role = guild.roles.cache.get(panel.role_id);
    const ping = role ? `${role}` : 'Staff';
    const welcomeText = panel.custom_message || `Welcome, ${interaction.user}! Staff will be with you shortly.`;
    const embed = new EmbedBuilder()
      .setColor(0x57f287)
      .setTitle(`Ticket #${number}`)
      .setDescription(welcomeText)
      .setFooter({ text: `Opened by ${interaction.user.username}` })
      .setTimestamp();
    const row = panels.buildTicketActionsRow(null);
    await ch.send({ content: `${ping}`, embeds: [embed], components: [row] });

    await logs.logToPanel(client, panelId, {
      title: 'Ticket opened',
      description: `Ticket #${number} opened by ${interaction.user}`,
      color: 0x57f287,
    });
    await panels.updatePanelMessage(client, panel);

    await interaction.editReply({ content: `Ticket created: ${ch}` });
    return;
  }

  // ----- Button: Open Ticket by Type -----
  if (interaction.customId.startsWith('ticket_open_type:')) {
    const typeId = parseInt(interaction.customId.split(':')[1], 10);
    const ticketType = db.getTicketType(typeId);
    if (!ticketType) return interaction.reply({ content: 'This ticket type no longer exists.', flags: MessageFlagsBitField.Flags.Ephemeral });

    const panel = db.getPanel(ticketType.panel_id);
    if (!panel) return interaction.reply({ content: 'This panel no longer exists.', flags: MessageFlagsBitField.Flags.Ephemeral });
    if (panel.disabled) return interaction.reply({ content: 'Tickets are temporarily disabled for this panel.', flags: MessageFlagsBitField.Flags.Ephemeral });
    if (db.getOpenTicketCount(panel.id) >= db.MAX_TICKETS_PER_PANEL) {
      return interaction.reply({ content: `This panel has reached the maximum of ${db.MAX_TICKETS_PER_PANEL} open tickets.`, flags: MessageFlagsBitField.Flags.Ephemeral });
    }

    // Check for duplicate if not allowed
    if (!ticketType.allow_duplicate && db.hasOpenTicketForType(typeId, interaction.user.id)) {
      return interaction.reply({ content: `You already have an open ${ticketType.name} ticket. Close it first to open another.`, flags: MessageFlagsBitField.Flags.Ephemeral });
    }

    // Check if there are questions - show modal if so
    const questions = db.getQuestionsByType(typeId);
    if (questions && questions.length > 0) {
      const modal = panels.buildTicketFormModal(ticketType, questions);
      await interaction.showModal(modal);
      return;
    }

    // No questions - create ticket directly
    await interaction.deferReply({ flags: MessageFlagsBitField.Flags.Ephemeral });
    const guild = interaction.guild;
    const categoryId = ticketType.category_id || panel.category_id || null;
    const ticketNum = db.getNextTicketNumber(panel.id);

    // Generate channel name from naming format
    let channelName = (ticketType.naming_format || '{type}-{number}')
      .replace('{type}', ticketType.name.toLowerCase().replace(/\s+/g, '-'))
      .replace('{number}', ticketNum)
      .replace('{user}', interaction.user.username.toLowerCase())
      .substring(0, 100);

    const overwrites = [
      { id: guild.id, type: 0, deny: [PermissionFlagsBits.ViewChannel] },
      { id: interaction.user.id, type: 1, allow: [PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages, PermissionFlagsBits.ReadMessageHistory] },
    ];

    // Add staff roles
    const staffRoles = ticketType.staff_role_ids || [panel.role_id];
    for (const roleId of staffRoles) {
      if (roleId) {
        overwrites.push({
          id: roleId,
          type: 0,
          allow: [PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages, PermissionFlagsBits.ReadMessageHistory],
        });
      }
    }

    const ch = await guild.channels.create({
      name: channelName,
      parent: categoryId,
      permissionOverwrites: overwrites,
    });

    const { id: ticketId, number } = db.createTicketWithType(panel.id, ch.id, interaction.user.id, typeId);

    // Build ping content
    const pingParts = [];
    for (const roleId of staffRoles) {
      const role = guild.roles.cache.get(roleId);
      if (role) pingParts.push(`${role}`);
    }
    const ping = pingParts.length > 0 ? pingParts.join(' ') : 'Staff';

    const welcomeText = ticketType.welcome_message || `Welcome, ${interaction.user}! Staff will be with you shortly.`;
    const embed = new EmbedBuilder()
      .setColor(0x57f287)
      .setTitle(`${ticketType.name} - Ticket #${number}`)
      .setDescription(welcomeText)
      .setFooter({ text: `Opened by ${interaction.user.username}` })
      .setTimestamp();

    const row = panels.buildTicketActionsRow(null);
    const row2 = panels.buildTicketActionsRow2(null);
    const components = row2 ? [row, row2] : [row];
    await ch.send({ content: ping, embeds: [embed], components });

    const logChannelId = ticketType.log_channel_id || panel.log_channel_id;
    if (logChannelId) {
      const logCh = guild.channels.cache.get(logChannelId);
      if (logCh) {
        const logEmbed = new EmbedBuilder()
          .setColor(0x57f287)
          .setTitle('Ticket opened')
          .setDescription(`**${ticketType.name}** Ticket #${number} opened by ${interaction.user}`)
          .setTimestamp();
        await logCh.send({ embeds: [logEmbed] }).catch(() => {});
      }
    }
    await panels.updatePanelMessage(client, panel);

    await interaction.editReply({ content: `Ticket created: ${ch}` });
    return;
  }

  if (interaction.customId === 'ticket_close') {
    const ticket = db.getTicketByChannel(interaction.channel.id);
    if (!ticket) return interaction.reply({ content: 'This is not a ticket channel.', flags: MessageFlagsBitField.Flags.Ephemeral });

    const modal = new ModalBuilder()
      .setCustomId('ticket_close_modal')
      .setTitle('Close ticket');
    modal.addComponents(
      new ActionRowBuilder().addComponents(
        new TextInputBuilder()
          .setCustomId('reason')
          .setLabel('Reason (optional)')
          .setPlaceholder('e.g. Resolved, duplicate, etc.')
          .setStyle(TextInputStyle.Paragraph)
          .setRequired(false)
      )
    );
    await interaction.showModal(modal);
    return;
  }

  if (interaction.customId === 'ticket_claim') {
    const ticket = db.getTicketByChannel(interaction.channel.id);
    if (!ticket) return interaction.reply({ content: 'This is not a ticket channel.', flags: MessageFlagsBitField.Flags.Ephemeral });
    if (ticket.claimed_by) return interaction.reply({ content: `Already claimed by <@${ticket.claimed_by}>.`, flags: MessageFlagsBitField.Flags.Ephemeral });

    await interaction.deferReply({ flags: MessageFlagsBitField.Flags.Ephemeral });
    db.setTicketClaimed(interaction.channel.id, interaction.user.id);
    await interaction.channel.send({ content: `**Ticket claimed by** ${interaction.user}` });
    await interaction.channel.send({ content: `<@${ticket.user_id}> Staff is with you — ${interaction.user} has claimed this ticket.` }).catch(() => {});
    const messages = await interaction.channel.messages.fetch({ limit: 10 });
    const msgWithButtons = messages.find((m) => m.components?.length > 0);
    if (msgWithButtons) {
      await msgWithButtons.edit({ components: [panels.buildTicketActionsRow(interaction.user.id)] }).catch(() => {});
    }
    await logs.logToPanel(client, ticket.panel_id, {
      title: 'Ticket claimed',
      description: `Ticket #${ticket.number} claimed by ${interaction.user}`,
      color: 0xfee75c,
    });
    await interaction.editReply({ content: 'You have claimed this ticket.' });
    return;
  }

  if (interaction.customId === 'ticket_unclaim') {
    const ticket = db.getTicketByChannel(interaction.channel.id);
    if (!ticket) return interaction.reply({ content: 'This is not a ticket channel.', flags: MessageFlagsBitField.Flags.Ephemeral });
    if (!ticket.claimed_by) return interaction.reply({ content: 'Ticket is not claimed.', flags: MessageFlagsBitField.Flags.Ephemeral });

    await interaction.deferReply({ flags: MessageFlagsBitField.Flags.Ephemeral });
    db.clearTicketClaimed(interaction.channel.id);
    await interaction.channel.send({ content: `**Ticket unclaimed by** ${interaction.user}` });
    const messages = await interaction.channel.messages.fetch({ limit: 10 });
    const msgWithButtons = messages.find((m) => m.components?.length > 0);
    if (msgWithButtons) await msgWithButtons.edit({ components: [panels.buildTicketActionsRow(null)] }).catch(() => {});
    await logs.logToPanel(client, ticket.panel_id, {
      title: 'Ticket unclaimed',
      description: `Ticket #${ticket.number} unclaimed by ${interaction.user}`,
      color: 0x5865f2,
    });
    await interaction.editReply({ content: 'Ticket unclaimed.' });
    return;
  }

  if (interaction.customId.startsWith('ticket_reopen:')) {
    const ticketId = parseInt(interaction.customId.split(':')[1], 10);
    const ticket = db.getTicket(ticketId);
    if (!ticket || ticket.status !== 'closed') {
      return interaction.reply({ content: 'Ticket not found or already open.', flags: MessageFlagsBitField.Flags.Ephemeral }).catch(() => {});
    }
    await interaction.deferReply({ flags: MessageFlagsBitField.Flags.Ephemeral });
    const panel = db.getPanel(ticket.panel_id);
    const guild = client.guilds.cache.get(panel?.guild_id);
    if (!panel || !guild) {
      return interaction.editReply({ content: 'Server not found.' }).catch(() => {});
    }
    await interaction.message.edit({ components: [panels.buildReopenRowDisabled(ticketId)] }).catch(() => {});

    const categoryId = panel.category_id || null;
    const newNum = db.getNextTicketNumber(panel.id);
    const channelName = `ticket-${newNum}`;
    const overwrites = [
      { id: guild.id, type: 0, deny: [PermissionFlagsBits.ViewChannel] },
      { id: ticket.user_id, type: 1, allow: [PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages, PermissionFlagsBits.ReadMessageHistory] },
    ];
    if (panel.role_id) {
      overwrites.push({
        id: panel.role_id,
        type: 0,
        allow: [PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages, PermissionFlagsBits.ReadMessageHistory],
      });
    }
    const ch = await guild.channels.create({
      name: channelName,
      parent: categoryId,
      permissionOverwrites: overwrites,
    });
    const { number } = db.createTicket(panel.id, ch.id, ticket.user_id);
    const role = guild.roles.cache.get(panel.role_id);
    const ping = role ? `${role}` : 'Staff';
    const userTag = guild.members.cache.get(ticket.user_id)?.user?.username || `<@${ticket.user_id}>`;
    const welcomeText = `Ticket reopened. Welcome back, ${userTag}!`;
    const embed = new EmbedBuilder()
      .setColor(0x57f287)
      .setTitle(`Ticket #${number} (reopened)`)
      .setDescription(welcomeText)
      .setFooter({ text: `Reopened by ${interaction.user.username}` })
      .setTimestamp();
    await ch.send({ content: `${ping}`, embeds: [embed], components: [panels.buildTicketActionsRow(null)] });
    await logs.logToPanel(client, panel.id, {
      title: 'Ticket reopened',
      description: `Ticket #${ticket.number} reopened as #${number} by ${interaction.user}`,
      color: 0x57f287,
    });
    await interaction.editReply({ content: `Ticket reopened: ${ch}` });
    return;
  }

  // ----- Button: Cancel Close -----
  if (interaction.customId === 'ticket_cancel_close') {
    const ticket = db.getTicketByChannel(interaction.channel.id);
    if (!ticket) return interaction.reply({ content: 'This is not a ticket channel.', flags: MessageFlagsBitField.Flags.Ephemeral });

    // Check if there's a pending deletion
    const pending = pendingDeletions.get(interaction.channel.id);
    if (!pending) {
      return interaction.reply({ content: 'No pending close to cancel.', flags: MessageFlagsBitField.Flags.Ephemeral });
    }

    // Clear the timeout
    clearTimeout(pending.timeout);
    pendingDeletions.delete(interaction.channel.id);

    // Update ticket status back to open
    db.setTicketStatus(interaction.channel.id, 'open');

    // Restore action buttons
    const messages = await interaction.channel.messages.fetch({ limit: 10 });
    const msgWithButtons = messages.find((m) => m.components?.length > 0);
    if (msgWithButtons) {
      const row = panels.buildTicketActionsRow(ticket.claimed_by);
      const row2 = panels.buildTicketActionsRow2(ticket.claimed_by);
      const components = row2 ? [row, row2] : [row];
      await msgWithButtons.edit({ components }).catch(() => {});
    }

    await interaction.reply({ content: 'Ticket close cancelled.', flags: MessageFlagsBitField.Flags.Ephemeral });
    await interaction.channel.send({ content: `**Ticket close cancelled by** ${interaction.user}` });

    await logs.logToPanel(client, ticket.panel_id, {
      title: 'Close cancelled',
      description: `Ticket #${ticket.number} close cancelled by ${interaction.user}`,
      color: 0x5865f2,
    });
    return;
  }
  } catch (err) {
    console.error('Button interaction error:', err);
    safeReply('Something went wrong. Try again.');
  }
});

// ----- Modals -----
client.on('interactionCreate', async (interaction) => {
  if (!interaction.isModalSubmit()) return;

  try {
  if (interaction.customId === 'ticket_close_modal') {
    const reason = interaction.fields.getTextInputValue('reason')?.trim() || null;
    const ticket = db.getTicketByChannel(interaction.channel.id);
    if (!ticket) return interaction.reply({ content: 'Not a ticket channel.', flags: MessageFlagsBitField.Flags.Ephemeral });

    await interaction.deferReply({ flags: MessageFlagsBitField.Flags.Ephemeral });
    const panel = db.getPanel(ticket.panel_id);
    const ticketType = ticket.ticket_type_id ? db.getTicketType(ticket.ticket_type_id) : null;
    const closedBy = interaction.user.username;

    // Mark as closing
    db.setTicketStatus(interaction.channel.id, 'closing');

    // Get configuration
    const deleteDelay = ticketType?.delete_delay_seconds ?? 5;
    const dmTranscript = ticketType?.dm_transcript ?? 0;
    const logChannelId = ticketType?.log_channel_id || panel.log_channel_id;
    const typeName = ticketType?.name || 'Ticket';

    // Generate transcripts (both HTML and text)
    let htmlTranscript = null;
    let textTranscript = null;
    try {
      htmlTranscript = await transcript.buildHtmlTranscript(interaction.channel, ticket);
      textTranscript = await transcript.buildTranscript(interaction.channel);
    } catch (e) {
      console.error('Transcript error:', e);
    }

    // Send to log channel
    if (logChannelId && htmlTranscript) {
      const logCh = interaction.guild.channels.cache.get(logChannelId);
      if (logCh) {
        const header = reason
          ? `Closed by: ${closedBy}\nReason: ${reason}`
          : `Closed by: ${closedBy}`;
        const buf = Buffer.from(htmlTranscript, 'utf8');
        const file = new AttachmentBuilder(buf, { name: `ticket-${ticket.number}-transcript.html` });
        const embed = new EmbedBuilder()
          .setColor(0xed4245)
          .setTitle(`${typeName} - Ticket #${ticket.number} closed`)
          .setDescription(header)
          .setTimestamp();
        const reopenRow = panels.buildReopenRow(ticket.id);
        await logCh.send({ embeds: [embed], files: [file], components: [reopenRow] }).catch(() => {});
      }
    }

    // DM user if enabled
    if (dmTranscript && htmlTranscript) {
      try {
        const user = await client.users.fetch(ticket.user_id).catch(() => null);
        if (user) {
          const buf = Buffer.from(htmlTranscript, 'utf8');
          const file = new AttachmentBuilder(buf, { name: `ticket-${ticket.number}-transcript.html` });
          const embed = new EmbedBuilder()
            .setColor(0xed4245)
            .setTitle(`Your ${typeName} ticket has been closed`)
            .setDescription(`Ticket #${ticket.number} in **${interaction.guild.name}** has been closed.\nReason: ${reason || 'No reason provided'}`)
            .setTimestamp();
          await user.send({ embeds: [embed], files: [file] }).catch(() => {});
        }
      } catch (e) {
        console.error('DM transcript error:', e);
      }
    }

    // Update action buttons to show cancel button
    const messages = await interaction.channel.messages.fetch({ limit: 10 });
    const msgWithButtons = messages.find((m) => m.components?.length > 0);
    if (msgWithButtons) {
      await msgWithButtons.edit({ components: [panels.buildClosingRow()] }).catch(() => {});
    }

    // Show closing embed with countdown
    const closingEmbed = new EmbedBuilder()
      .setColor(0xed4245)
      .setTitle('Ticket Closing')
      .setDescription(`This ticket will be deleted in **${deleteDelay} seconds**.\nClick "Cancel Close" to abort.`)
      .setTimestamp();
    await interaction.channel.send({ embeds: [closingEmbed] }).catch(() => {});

    await interaction.editReply({ content: 'Ticket closing...' }).catch(() => {});

    // Set up delayed deletion
    const timeoutId = setTimeout(async () => {
      pendingDeletions.delete(interaction.channel.id);
      db.closeTicket(interaction.channel.id);
      await panels.updatePanelMessage(client, panel);
      await interaction.channel.delete().catch(() => {});
    }, deleteDelay * 1000);

    // Store for cancellation
    pendingDeletions.set(interaction.channel.id, {
      timeout: timeoutId,
      ticketId: ticket.id,
    });

    return;
  }

  // ----- Modal: Ticket Form (from ticket type questions) -----
  if (interaction.customId.startsWith('ticket_form:')) {
    const typeId = parseInt(interaction.customId.split(':')[1], 10);
    const ticketType = db.getTicketType(typeId);
    if (!ticketType) return interaction.reply({ content: 'This ticket type no longer exists.', flags: MessageFlagsBitField.Flags.Ephemeral });

    const panel = db.getPanel(ticketType.panel_id);
    if (!panel) return interaction.reply({ content: 'This panel no longer exists.', flags: MessageFlagsBitField.Flags.Ephemeral });
    if (panel.disabled) return interaction.reply({ content: 'Tickets are temporarily disabled for this panel.', flags: MessageFlagsBitField.Flags.Ephemeral });
    if (db.getOpenTicketCount(panel.id) >= db.MAX_TICKETS_PER_PANEL) {
      return interaction.reply({ content: `This panel has reached the maximum of ${db.MAX_TICKETS_PER_PANEL} open tickets.`, flags: MessageFlagsBitField.Flags.Ephemeral });
    }

    // Re-check duplicate (in case user had modal open while another ticket was created)
    if (!ticketType.allow_duplicate && db.hasOpenTicketForType(typeId, interaction.user.id)) {
      return interaction.reply({ content: `You already have an open ${ticketType.name} ticket.`, flags: MessageFlagsBitField.Flags.Ephemeral });
    }

    await interaction.deferReply({ flags: MessageFlagsBitField.Flags.Ephemeral });

    // Get questions and extract responses
    const questions = db.getQuestionsByType(typeId);
    const responses = [];
    for (const q of questions) {
      const value = interaction.fields.getTextInputValue(`question_${q.id}`)?.trim() || '';
      responses.push({ question_id: q.id, response: value, label: q.question_label });
    }

    const guild = interaction.guild;
    const categoryId = ticketType.category_id || panel.category_id || null;
    const ticketNum = db.getNextTicketNumber(panel.id);

    // Generate channel name from naming format
    let channelName = (ticketType.naming_format || '{type}-{number}')
      .replace('{type}', ticketType.name.toLowerCase().replace(/\s+/g, '-'))
      .replace('{number}', ticketNum)
      .replace('{user}', interaction.user.username.toLowerCase())
      .substring(0, 100);

    const overwrites = [
      { id: guild.id, type: 0, deny: [PermissionFlagsBits.ViewChannel] },
      { id: interaction.user.id, type: 1, allow: [PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages, PermissionFlagsBits.ReadMessageHistory] },
    ];

    // Add staff roles
    const staffRoles = ticketType.staff_role_ids || [panel.role_id];
    for (const roleId of staffRoles) {
      if (roleId) {
        overwrites.push({
          id: roleId,
          type: 0,
          allow: [PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages, PermissionFlagsBits.ReadMessageHistory],
        });
      }
    }

    const ch = await guild.channels.create({
      name: channelName,
      parent: categoryId,
      permissionOverwrites: overwrites,
    });

    const { id: ticketId, number } = db.createTicketWithType(panel.id, ch.id, interaction.user.id, typeId);

    // Save form responses
    if (responses.length > 0) {
      db.saveTicketResponses(ticketId, responses);
    }

    // Build ping content
    const pingParts = [];
    for (const roleId of staffRoles) {
      const role = guild.roles.cache.get(roleId);
      if (role) pingParts.push(`${role}`);
    }
    const ping = pingParts.length > 0 ? pingParts.join(' ') : 'Staff';

    // Build welcome embed with form responses
    const welcomeText = ticketType.welcome_message || `Welcome, ${interaction.user}! Staff will be with you shortly.`;
    const embed = new EmbedBuilder()
      .setColor(0x57f287)
      .setTitle(`${ticketType.name} - Ticket #${number}`)
      .setDescription(welcomeText)
      .setFooter({ text: `Opened by ${interaction.user.username}` })
      .setTimestamp();

    // Add form responses as fields
    for (const r of responses) {
      if (r.response) {
        embed.addFields({ name: r.label, value: r.response.substring(0, 1024), inline: false });
      }
    }

    const row = panels.buildTicketActionsRow(null);
    const row2 = panels.buildTicketActionsRow2(null);
    const components = row2 ? [row, row2] : [row];
    await ch.send({ content: ping, embeds: [embed], components });

    const logChannelId = ticketType.log_channel_id || panel.log_channel_id;
    if (logChannelId) {
      const logCh = guild.channels.cache.get(logChannelId);
      if (logCh) {
        const logEmbed = new EmbedBuilder()
          .setColor(0x57f287)
          .setTitle('Ticket opened')
          .setDescription(`**${ticketType.name}** Ticket #${number} opened by ${interaction.user}`)
          .setTimestamp();
        await logCh.send({ embeds: [logEmbed] }).catch(() => {});
      }
    }
    await panels.updatePanelMessage(client, panel);

    await interaction.editReply({ content: `Ticket created: ${ch}` });
    return;
  }

  } catch (err) {
    console.error('Modal interaction error:', err);
    try {
      if (interaction.deferred) await interaction.editReply({ content: 'Something went wrong. Try again.' }).catch(() => {});
      else await interaction.reply({ content: 'Something went wrong. Try again.', flags: MessageFlagsBitField.Flags.Ephemeral }).catch(() => {});
    } catch (_) {}
  }
});

// ----- Slash: /add, /remove, /rename, /close (ticket channels only) -----
const closeRequestPending = new Map();

client.on('interactionCreate', async (interaction) => {
  if (!interaction.isChatInputCommand()) return;
  const name = interaction.commandName;
  if (!['add', 'remove', 'rename', 'close'].includes(name)) return;

  const ticket = db.getTicketByChannel(interaction.channel?.id);
  if (!ticket) {
    return interaction.reply({ content: 'Use this command in a ticket channel.', flags: MessageFlagsBitField.Flags.Ephemeral }).catch(() => {});
  }

  try {
    if (name === 'add') {
      const user = interaction.options.getUser('user');
      if (!user) return interaction.reply({ content: 'User is required.', flags: MessageFlagsBitField.Flags.Ephemeral });
      try {
        await interaction.channel.permissionOverwrites.edit(user.id, {
          ViewChannel: true,
          SendMessages: true,
          ReadMessageHistory: true,
        });
        await interaction.reply({ content: `Added ${user.username} to the ticket.`, flags: MessageFlagsBitField.Flags.Ephemeral });
        await logs.logToPanel(client, ticket.panel_id, {
          title: 'Player added',
          description: `User ${user.username} added to Ticket #${ticket.number} by ${interaction.user}`,
          color: 0x5865f2,
        });
      } catch (e) {
        await interaction.reply({ content: 'Could not add user. Check permissions.', flags: MessageFlagsBitField.Flags.Ephemeral });
      }
      return;
    }

    if (name === 'remove') {
      const user = interaction.options.getUser('user');
      if (!user) return interaction.reply({ content: 'User is required.', flags: MessageFlagsBitField.Flags.Ephemeral });
      if (user.id === ticket.user_id) {
        return interaction.reply({ content: 'Cannot remove the ticket owner.', flags: MessageFlagsBitField.Flags.Ephemeral });
      }
      try {
        await interaction.channel.permissionOverwrites.delete(user.id);
        await interaction.reply({ content: `Removed ${user.username} from the ticket.`, flags: MessageFlagsBitField.Flags.Ephemeral });
        await logs.logToPanel(client, ticket.panel_id, {
          title: 'Player removed',
          description: `User ${user.username} removed from Ticket #${ticket.number} by ${interaction.user}`,
          color: 0xed4245,
        });
      } catch (e) {
        await interaction.reply({ content: 'Could not remove user.', flags: MessageFlagsBitField.Flags.Ephemeral });
      }
      return;
    }

    if (name === 'rename') {
      let newName = interaction.options.getString('name').trim().toLowerCase().replace(/\s+/g, '-').slice(0, 100);
      if (!newName) return interaction.reply({ content: 'Name cannot be empty.', flags: MessageFlagsBitField.Flags.Ephemeral });
      try {
        await interaction.channel.setName(newName);
        await interaction.reply({ content: `Channel renamed to **${newName}**.`, flags: MessageFlagsBitField.Flags.Ephemeral });
        await logs.logToPanel(client, ticket.panel_id, {
          title: 'Ticket renamed',
          description: `Ticket #${ticket.number} renamed to **${newName}** by ${interaction.user}`,
          color: 0x5865f2,
        });
      } catch (e) {
        await interaction.reply({ content: 'Could not rename. Use lowercase, no spaces.', flags: MessageFlagsBitField.Flags.Ephemeral });
      }
      return;
    }

    if (name === 'close') {
      const sub = interaction.options.getSubcommand();
      const panel = db.getPanel(ticket.panel_id);
      const ticketType = ticket.ticket_type_id ? db.getTicketType(ticket.ticket_type_id) : null;
      const deleteDelay = 3;
      const reason = interaction.options.getString('reason')?.trim() || null;

      if (sub === 'now') {
        db.setTicketStatus(interaction.channel.id, 'closing');
        const messages = await interaction.channel.messages.fetch({ limit: 10 });
        const msgWithButtons = messages.find((m) => m.components?.length > 0);
        if (msgWithButtons) await msgWithButtons.edit({ components: [panels.buildClosingRow()] }).catch(() => {});
        const closingEmbed = new EmbedBuilder()
          .setColor(0xed4245)
          .setTitle('Ticket Closing')
          .setDescription(`This ticket will be deleted in **${deleteDelay} seconds**.\nClick "Cancel Close" to abort.${reason ? `\nReason: ${reason}` : ''}`)
          .setTimestamp();
        await interaction.channel.send({ embeds: [closingEmbed] }).catch(() => {});
        await interaction.reply({ content: 'Ticket closing...', flags: MessageFlagsBitField.Flags.Ephemeral }).catch(() => {});

        const timeoutId = setTimeout(async () => {
          pendingDeletions.delete(interaction.channel.id);
          await closeTicketWithTranscript(client, ticket, interaction.user.username + (reason ? ` — ${reason}` : ''));
        }, deleteDelay * 1000);
        pendingDeletions.set(interaction.channel.id, { timeout: timeoutId, ticketId: ticket.id });
        return;
      }

      if (sub === 'request') {
        const duration = interaction.options.getInteger('duration') || null;
        const openerId = ticket.user_id;
        const row = new ActionRowBuilder().addComponents(
          new ButtonBuilder().setCustomId('close_request_accept').setLabel('Accept').setStyle(ButtonStyle.Success),
          new ButtonBuilder().setCustomId('close_request_deny').setLabel('Deny').setStyle(ButtonStyle.Danger)
        );
        const msgText = `<@${openerId}> **Close request** from ${interaction.user}\n${reason ? `Reason: ${reason}\n` : ''}${duration ? `Auto-close in ${duration}s if no response.` : 'Accept or deny below.'}`;
        const msg = await interaction.channel.send({
          content: msgText,
          components: [row],
        });
        await interaction.reply({ content: 'Close request sent to the ticket opener.', flags: MessageFlagsBitField.Flags.Ephemeral });

        let resolved = false;
        const resolve = () => {
          if (resolved) return;
          resolved = true;
          closeRequestPending.delete(interaction.channel.id);
        };

        const timeoutId = duration
          ? setTimeout(async () => {
              resolve();
              await closeTicketWithTranscript(client, ticket, interaction.user.username + ' (request auto-accepted)' + (reason ? ` — ${reason}` : ''));
            }, duration * 1000)
          : null;

        closeRequestPending.set(interaction.channel.id, {
          ticket,
          reason,
          requestedBy: interaction.user.username,
          messageId: msg.id,
          timeoutId,
          resolve,
        });
        return;
      }
    }
  } catch (err) {
    console.error('Ticket slash command error:', err);
    try {
      await interaction.reply({ content: 'Something went wrong. Try again.', flags: MessageFlagsBitField.Flags.Ephemeral }).catch(() => {});
    } catch (_) {}
  }
});

// ----- Buttons: close request accept/deny -----
client.on('interactionCreate', async (interaction) => {
  if (!interaction.isButton()) return;
  if (interaction.customId !== 'close_request_accept' && interaction.customId !== 'close_request_deny') return;

  const pending = closeRequestPending.get(interaction.channel.id);
  if (!pending) return interaction.reply({ content: 'This request has expired.', flags: MessageFlagsBitField.Flags.Ephemeral }).catch(() => {});

  const ticket = db.getTicketByChannel(interaction.channel.id);
  if (!ticket) return;

  if (interaction.user.id !== ticket.user_id) {
    return interaction.reply({ content: 'Only the ticket opener can accept or deny the close request.', flags: MessageFlagsBitField.Flags.Ephemeral }).catch(() => {});
  }

  if (interaction.customId === 'close_request_deny') {
    pending.resolve();
    if (pending.timeoutId) clearTimeout(pending.timeoutId);
    await interaction.update({ content: interaction.message.content + '\n\n**Close denied** by ' + interaction.user.username, components: [] }).catch(() => {});
    return;
  }

  if (interaction.customId === 'close_request_accept') {
    pending.resolve();
    if (pending.timeoutId) clearTimeout(pending.timeoutId);
    await interaction.update({ content: interaction.message.content + '\n\n**Close accepted** by ' + interaction.user.username, components: [] }).catch(() => {});

    db.setTicketStatus(interaction.channel.id, 'closing');
    const messages = await interaction.channel.messages.fetch({ limit: 10 });
    const msgWithButtons = messages.find((m) => m.components?.length > 0);
    if (msgWithButtons) await msgWithButtons.edit({ components: [panels.buildClosingRow()] }).catch(() => {});
    const deleteDelay = 3;
    const closingEmbed = new EmbedBuilder()
      .setColor(0xed4245)
      .setTitle('Ticket Closing')
      .setDescription(`Closing in **${deleteDelay} seconds**.`)
      .setTimestamp();
    await interaction.channel.send({ embeds: [closingEmbed] }).catch(() => {});

    const timeoutId = setTimeout(async () => {
      pendingDeletions.delete(interaction.channel.id);
      await closeTicketWithTranscript(client, ticket, interaction.user.username + ' (accepted request)' + (pending.reason ? ` — ${pending.reason}` : ''));
    }, deleteDelay * 1000);
    pendingDeletions.set(interaction.channel.id, { timeout: timeoutId, ticketId: ticket.id });
  }
});

// ----- Message: update last_message_at for auto-close -----
client.on('messageCreate', async (message) => {
  if (message.author.bot) return;
  const ticket = db.getTicketByChannel(message.channel.id);
  if (ticket) db.updateTicketLastMessage(message.channel.id);
});

// ----- Auto-close job -----
async function runAutoClose(client) {
  const list = db.getTicketsForAutoClose();
  for (const ticket of list) {
    await closeTicketWithTranscript(client, ticket, 'Auto-close (inactive)');
  }
}

function startAutoClose(client) {
  // Run once on startup
  runAutoClose(client).catch((err) => console.error('Startup auto-close error:', err));
  // Then every hour
  const intervalMs = 60 * 60 * 1000;
  setInterval(() => runAutoClose(client).catch((err) => console.error('Auto-close error:', err)), intervalMs);
}

// ----- Graceful shutdown -----
function shutdown() {
  console.log('Shutting down METiers...');
  // Clear all pending deletion timeouts
  for (const [channelId, pending] of pendingDeletions) {
    clearTimeout(pending.timeout);
  }
  pendingDeletions.clear();
  for (const [channelId, pending] of closeRequestPending) {
    if (pending.timeoutId) clearTimeout(pending.timeoutId);
  }
  closeRequestPending.clear();
  // Close database
  try { db.db.close(); } catch (_) {}
  client.destroy();
  process.exit(0);
}
process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);

client.login(process.env.DISCORD_TOKEN).catch((err) => {
  console.error('Login failed:', err.message);
  process.exit(1);
});
