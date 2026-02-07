const { EmbedBuilder, ActionRowBuilder, ButtonBuilder, ButtonStyle, ModalBuilder, TextInputBuilder, TextInputStyle } = require('discord.js');
const db = require('./db');

function parseEmbedColor(hex) {
  if (!hex || typeof hex !== 'string') return 0x5865f2;
  const s = hex.replace(/^#/, '');
  if (!/^[0-9a-fA-F]{6}$/.test(s)) return 0x5865f2;
  return parseInt(s, 16);
}

function buildPanelEmbed(panel) {
  return new EmbedBuilder()
    .setColor(parseEmbedColor(panel.embed_color))
    .setTitle(panel.title)
    .setDescription(panel.description);
}

// Map string button style to Discord ButtonStyle
function mapButtonStyle(style) {
  const styleMap = {
    'Primary': ButtonStyle.Primary,
    'Secondary': ButtonStyle.Secondary,
    'Success': ButtonStyle.Success,
    'Danger': ButtonStyle.Danger,
  };
  return styleMap[style] || ButtonStyle.Primary;
}

function buildPanelRow(panel) {
  const ticketTypes = db.getTicketTypesByPanel(panel.id);
  const disabled = !!panel.disabled;
  if (ticketTypes && ticketTypes.length > 0) {
    return buildMultiButtonPanelRow(ticketTypes, panel);
  }
  const btn = new ButtonBuilder()
    .setCustomId(`ticket_open:${panel.id}`)
    .setLabel(panel.button_label)
    .setStyle(ButtonStyle.Primary)
    .setDisabled(disabled);
  if (panel.button_emoji) {
    btn.setEmoji(panel.button_emoji.trim());
  }
  return new ActionRowBuilder().addComponents(btn);
}

// Build panel row with multiple ticket type buttons (up to 5)
function buildMultiButtonPanelRow(ticketTypes, panel) {
  const disabled = panel ? !!panel.disabled : false;
  const buttons = ticketTypes.slice(0, 5).map(type => {
    const btn = new ButtonBuilder()
      .setCustomId(`ticket_open_type:${type.id}`)
      .setLabel(type.name)
      .setStyle(mapButtonStyle(type.button_style))
      .setDisabled(disabled);
    if (type.emoji) {
      const emoji = type.emoji.trim();
      btn.setEmoji(emoji);
    }
    return btn;
  });
  return new ActionRowBuilder().addComponents(buttons);
}

function buildTicketActionsRow(claimedBy = null, isClosing = false) {
  if (isClosing) {
    return buildClosingRow();
  }
  // Only Close and Claim/Unclaim; use /add, /remove, /rename in ticket channels
  const row = [
    new ButtonBuilder().setCustomId('ticket_close').setLabel('Close Ticket').setStyle(ButtonStyle.Danger),
    new ButtonBuilder().setCustomId('ticket_claim').setLabel(claimedBy ? 'Claimed' : 'Claim').setStyle(claimedBy ? ButtonStyle.Success : ButtonStyle.Primary).setDisabled(!!claimedBy),
  ];
  const actionRow = new ActionRowBuilder().addComponents(row);
  return actionRow;
}

// Second row for unclaim button when ticket is claimed
function buildTicketActionsRow2(claimedBy = null) {
  if (!claimedBy) return null;
  return new ActionRowBuilder().addComponents(
    new ButtonBuilder().setCustomId('ticket_unclaim').setLabel('Unclaim').setStyle(ButtonStyle.Secondary)
  );
}

// Row shown during close delay countdown
function buildClosingRow() {
  return new ActionRowBuilder().addComponents(
    new ButtonBuilder().setCustomId('ticket_cancel_close').setLabel('Cancel Close').setStyle(ButtonStyle.Primary)
  );
}

function buildReopenRow(ticketId) {
  return new ActionRowBuilder().addComponents(
    new ButtonBuilder()
      .setCustomId(`ticket_reopen:${ticketId}`)
      .setLabel('Reopen Ticket')
      .setStyle(ButtonStyle.Primary)
  );
}

// Build modal for ticket form (up to 5 questions)
function buildTicketFormModal(ticketType, questions) {
  const modal = new ModalBuilder()
    .setCustomId(`ticket_form:${ticketType.id}`)
    .setTitle(`${ticketType.name} Ticket`);

  const rows = questions.slice(0, 5).map((q, index) => {
    const style = q.question_style === 'Paragraph' ? TextInputStyle.Paragraph : TextInputStyle.Short;
    const input = new TextInputBuilder()
      .setCustomId(`question_${q.id}`)
      .setLabel(q.question_label.substring(0, 45)) // Label max 45 chars
      .setStyle(style)
      .setRequired(!!q.required);

    if (q.question_placeholder) {
      input.setPlaceholder(q.question_placeholder.substring(0, 100));
    }
    if (q.min_length) {
      input.setMinLength(q.min_length);
    }
    if (q.max_length) {
      input.setMaxLength(q.max_length);
    } else {
      // Default max length based on style
      input.setMaxLength(style === TextInputStyle.Paragraph ? 1024 : 256);
    }

    return new ActionRowBuilder().addComponents(input);
  });

  modal.addComponents(...rows);
  return modal;
}

function buildReopenRowDisabled(ticketId) {
  return new ActionRowBuilder().addComponents(
    new ButtonBuilder()
      .setCustomId(`ticket_reopen:${ticketId}`)
      .setLabel('Reopened')
      .setStyle(ButtonStyle.Secondary)
      .setDisabled(true)
  );
}

async function updatePanelMessage(client, panel) {
  if (!panel.message_id || !panel.channel_id) return;
  const guild = client.guilds.cache.get(panel.guild_id);
  if (!guild) return;
  const ch = guild.channels.cache.get(panel.channel_id);
  if (!ch) return;
  const msg = await ch.messages.fetch(panel.message_id).catch(() => null);
  if (!msg) return;
  const embed = buildPanelEmbed(panel);
  const row = buildPanelRow(panel);
  await msg.edit({ embeds: [embed], components: [row] }).catch(() => {});
}

async function postPanel(channel, panelData) {
  const embed = buildPanelEmbed(panelData);
  const row = buildPanelRow(panelData);
  const msg = await channel.send({ embeds: [embed], components: [row] });
  return msg.id;
}

module.exports = {
  buildPanelEmbed,
  buildPanelRow,
  buildMultiButtonPanelRow,
  buildTicketActionsRow,
  buildTicketActionsRow2,
  buildClosingRow,
  buildReopenRow,
  buildReopenRowDisabled,
  buildTicketFormModal,
  updatePanelMessage,
  postPanel,
  parseEmbedColor,
  mapButtonStyle,
};
