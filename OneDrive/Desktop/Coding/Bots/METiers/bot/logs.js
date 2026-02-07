const { EmbedBuilder } = require('discord.js');
const db = require('./db');

async function logToPanel(client, panelId, opts) {
  const panel = db.getPanel(panelId);
  if (!panel || !panel.log_channel_id) return;
  const guild = client.guilds.cache.get(panel.guild_id);
  if (!guild) return;
  const ch = guild.channels.cache.get(panel.log_channel_id);
  if (!ch) return;
  const embed = new EmbedBuilder()
    .setColor(opts.color ?? 0x5865f2)
    .setTitle(opts.title ?? 'Ticket log')
    .setDescription(opts.description ?? '')
    .setTimestamp();
  if (opts.fields) embed.addFields(opts.fields);
  if (opts.footer) embed.setFooter({ text: opts.footer });
  const payload = { embeds: [embed] };
  if (opts.files?.length) payload.files = opts.files;
  await ch.send(payload).catch(() => {});
}

module.exports = { logToPanel };
