const { SlashCommandBuilder, PermissionFlagsBits, EmbedBuilder, ChannelType, MessageFlags } = require('discord.js');
const db = require('./db');

const ALLOWED_TIERS = [
  'LT5', 'HT5', 'LT4', 'HT4', 'LT3', 'HT3', 'LT2', 'HT2', 'LT1', 'HT1',
  'RHT1', 'RHT2', 'RLT1', 'RLT2',
  'UNR',
];

// Valid kits - must match website MODES exactly
const VALID_KITS = [
  { name: 'Sword', value: 'sword' },
  { name: 'Axe', value: 'axe' },
  { name: 'Netherite Pot', value: 'netherite_pot' },
  { name: 'Diamond Pot', value: 'diamond_pot' },
  { name: 'Vanilla', value: 'vanilla' },
  { name: 'Mace', value: 'mace' },
  { name: 'UHC', value: 'uhc' },
  { name: 'SMP', value: 'smp' },
];

const TIER_COLORS = {
  LT5: 0x95a5a6, HT5: 0x5865f2, LT4: 0x57f287, HT4: 0x3498db, LT3: 0xfee75c,
  HT3: 0xe67e22, LT2: 0xed4245, HT2: 0xe91e63, LT1: 0x9b59b6, HT1: 0xf1c40f,
  RHT1: 0x607d8b, RHT2: 0x607d8b, RLT1: 0x607d8b, RLT2: 0x607d8b,
  UNR: 0x78909c,
};

function getCommands() {
  const tester = new SlashCommandBuilder()
    .setName('tester')
    .setDescription('Manage tier testers (admin only for add/remove)')
    .setDefaultMemberPermissions(PermissionFlagsBits.ManageGuild)
    .addSubcommand((sub) =>
      sub
        .setName('add')
        .setDescription('Add a user as a tester')
        .addUserOption((o) => o.setName('user').setDescription('User to add as tester').setRequired(true))
    )
    .addSubcommand((sub) =>
      sub
        .setName('remove')
        .setDescription('Remove a user from testers')
        .addUserOption((o) => o.setName('user').setDescription('User to remove').setRequired(true))
    )
    .addSubcommand((sub) =>
      sub.setName('list').setDescription('List all testers')
    );

  const tierChoices = ALLOWED_TIERS.map((t) => ({ name: t, value: t }));

  const result = new SlashCommandBuilder()
    .setName('result')
    .setDescription('Set or view tier test result for a user')
    .addUserOption((o) => o.setName('user').setDescription('User to set or view tier for').setRequired(true))
    .addStringOption((o) =>
      o.setName('kit').setDescription('Kit/mode tested').setRequired(true).addChoices(...VALID_KITS)
    )
    .addStringOption((o) =>
      o.setName('tier').setDescription('Tier (LT5-HT1). Omit to view current tier.').setRequired(false).addChoices(...tierChoices)
    );

  const setCmd = new SlashCommandBuilder()
    .setName('set')
    .setDescription('Set a user\'s tier in one command (tester only)')
    .addUserOption((o) => o.setName('user').setDescription('User who was tested').setRequired(true))
    .addStringOption((o) => o.setName('kit').setDescription('Kit/mode tested').setRequired(true).addChoices(...VALID_KITS))
    .addStringOption((o) => o.setName('rank').setDescription('Tier (LT5-HT1)').setRequired(true).addChoices(...tierChoices));

  const admin = new SlashCommandBuilder()
    .setName('admin')
    .setDescription('METiers: configure tester role and allowed tiers')
    .setDefaultMemberPermissions(PermissionFlagsBits.ManageGuild)
    .addSubcommand((sub) =>
      sub
        .setName('tester-role')
        .setDescription('Set the role that counts as tester (having this role = can use /result)')
        .addRoleOption((o) => o.setName('role').setDescription('Tester role (leave empty to clear)').setRequired(false))
    )
    .addSubcommand((sub) =>
      sub
        .setName('results-channel')
        .setDescription('Channel where tier results are posted (required for /result broadcasts)')
        .addChannelOption((o) =>
          o.setName('channel').setDescription('Results channel').setRequired(true).addChannelTypes(ChannelType.GuildText)
        )
    )
    .addSubcommand((sub) =>
      sub.setName('config').setDescription('Show current tester/tier config')
    )
    .addSubcommand((sub) =>
      sub.setName('info').setDescription('METiers admin: what you can do here')
    )
    .addSubcommand((sub) =>
      sub
        .setName('clear')
        .setDescription('Clear a user\'s tier result')
        .addUserOption((o) => o.setName('user').setDescription('User to clear tier for').setRequired(true))
    )
    .addSubcommand((sub) =>
      sub
        .setName('setkit')
        .setDescription('Add a kit/mode for tier testing')
        .addStringOption((o) => o.setName('name').setDescription('Kit name (e.g. sword, axe)').setRequired(true))
        .addStringOption((o) => o.setName('label').setDescription('Display label (e.g. Sword, Diamond Axe)').setRequired(true))
        .addStringOption((o) => o.setName('emoji').setDescription('Emoji for the kit').setRequired(false))
    )
    .addSubcommand((sub) =>
      sub
        .setName('removekit')
        .setDescription('Remove a kit from the list')
        .addStringOption((o) => o.setName('name').setDescription('Kit name to remove').setRequired(true))
    )
    .addSubcommand((sub) =>
      sub.setName('kits').setDescription('List all configured kits')
    );

  const tiers = new SlashCommandBuilder()
    .setName('tiers')
    .setDescription('View all tier results for a user across all kits')
    .addUserOption((o) => o.setName('user').setDescription('User to view tiers for').setRequired(true));

  return [tester.toJSON(), result.toJSON(), setCmd.toJSON(), admin.toJSON(), tiers.toJSON()];
}

function canSetResult(interaction) {
  const member = interaction.member;
  if (member.permissions.has(PermissionFlagsBits.ManageGuild)) return true;
  return db.isTester(interaction.guildId, interaction.user.id, member);
}

function normalizeTier(input) {
  const lower = input.trim().toUpperCase();
  return ALLOWED_TIERS.find((t) => t === lower) || null;
}

function replyOrEdit(interaction, content, ephemeral = true) {
  const payload = typeof content === 'string' ? { content } : content;
  if (interaction.deferred) return interaction.editReply(payload);
  return interaction.reply({ ...payload, flags: ephemeral ? MessageFlags.Ephemeral : undefined });
}

async function applyTierSet(interaction, user, tierValue, kit) {
  if (!canSetResult(interaction)) {
    return replyOrEdit(interaction, 'You need to be a tester or have Manage Server to set tier results.');
  }
  const tier = normalizeTier(tierValue);
  if (!tier) {
    return replyOrEdit(interaction, `Invalid tier. Allowed: **${ALLOWED_TIERS.join(', ')}**`);
  }

  // Validate kit - required and must be in VALID_KITS
  if (!kit) {
    return replyOrEdit(interaction, 'Kit is required. Please select a kit/mode.');
  }
  const kitInfo = VALID_KITS.find(k => k.value === kit.toLowerCase());
  if (!kitInfo) {
    const validKitNames = VALID_KITS.map(k => k.name).join(', ');
    return replyOrEdit(interaction, `Invalid kit **${kit}**. Valid kits: ${validKitNames}`);
  }

  const mode = kitInfo.value;
  const oldRow = db.getUserTier(interaction.guildId, user.id, mode);
  const oldTier = oldRow ? oldRow.tier : null;
  db.setUserTier(interaction.guildId, user.id, tier, interaction.user.id, mode);

  const config = db.getTesterConfig(interaction.guildId);
  const resultsChannelId = config?.results_channel_id;
  if (resultsChannelId) {
    try {
      const ch = await interaction.client.channels.fetch(resultsChannelId).catch(() => null);
      if (ch) {
        const avatarUrl = user.displayAvatarURL({ size: 256 });
        const color = TIER_COLORS[tier] ?? 0x5865f2;
        const embed = new EmbedBuilder()
          .setTitle('Tier result')
          .setColor(color)
          .setThumbnail(avatarUrl)
          .addFields(
            { name: 'User', value: `${user.username}`, inline: true },
            { name: 'Kit', value: kitInfo.name, inline: true },
            { name: 'Old tier', value: oldTier ?? 'N/A', inline: true },
            { name: 'New tier', value: tier, inline: true },
            { name: 'Tester', value: `${interaction.user}`, inline: false }
          )
          .setTimestamp();
        await ch.send({ content: `${user}`, embeds: [embed] });
      }
    } catch (err) {
      console.error('Results channel send error:', err);
    }
  }

  let siteNote = '';
  const webhookUrl = process.env.SITE_SYNC_WEBHOOK_URL;
  if (webhookUrl && process.env.SITE_SYNC_SECRET) {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 4000);
    try {
      const res = await fetch(webhookUrl, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Sync-Secret': process.env.SITE_SYNC_SECRET,
        },
        body: JSON.stringify({
          userId: user.id,
          username: user.username,
          tier,
          kit: kitInfo.value,
          guildId: interaction.guildId,
          testerTag: interaction.user.username,
        }),
        signal: controller.signal,
      });
      clearTimeout(timeout);
      if (res.ok) {
        siteNote = ' Site updated.';
      } else {
        siteNote = ' Site did not accept update; use Sync from Discord in Admin if needed.';
      }
    } catch (err) {
      clearTimeout(timeout);
      if (err?.cause?.code === 'ECONNREFUSED' || err?.name === 'AbortError') {
        console.warn('Site sync webhook: site unreachable or slow. Tier was still saved.');
        siteNote = ' Site unreachable—use Sync from Discord in Admin to update the website.';
      } else {
        console.error('Site sync webhook error:', err);
        siteNote = ' Site sync failed; use Sync from Discord in Admin if needed.';
      }
    }
  }

  return replyOrEdit(interaction, `Set **${user.username}** to **${tier}** (${kitInfo.name}).${siteNote}`);
}

function register(client) {
  client.on('interactionCreate', async (interaction) => {
    if (!interaction.isChatInputCommand()) return;
    const name = interaction.commandName;

    try {
    if (name === 'tester') {
      const sub = interaction.options.getSubcommand();
      if (sub === 'add') {
        const user = interaction.options.getUser('user');
        db.addTester(interaction.guildId, user.id);
        return interaction.reply({ content: `Added ${user.username} as a tester.`, flags: MessageFlags.Ephemeral });
      }
      if (sub === 'remove') {
        const user = interaction.options.getUser('user');
        db.removeTester(interaction.guildId, user.id);
        return interaction.reply({ content: `Removed ${user.username} from testers.`, flags: MessageFlags.Ephemeral });
      }
      if (sub === 'list') {
        const config = db.getTesterConfig(interaction.guildId);
        const testers = db.getTesters(interaction.guildId);
        const roleId = config?.tester_role_id;
        const roleMention = roleId ? `<@&${roleId}>` : null;
        const userList = testers.map((r) => `<@${r.user_id}>`).join(', ') || '(none)';
        let text = `**Testers (by list):** ${userList}`;
        if (roleMention) text += `\n**Tester role:** ${roleMention} (anyone with this role can use /result)`;
        return interaction.reply({ content: text, flags: MessageFlags.Ephemeral });
      }
    }

    if (name === 'set') {
      const user = interaction.options.getUser('user');
      const rank = interaction.options.getString('rank')?.trim();
      const kit = interaction.options.getString('kit');
      if (!rank) return interaction.reply({ content: 'Rank is required.', flags: MessageFlags.Ephemeral });
      await interaction.deferReply({ flags: MessageFlags.Ephemeral });
      return applyTierSet(interaction, user, rank, kit);
    }

    if (name === 'result') {
      const user = interaction.options.getUser('user');
      const tier = interaction.options.getString('tier');
      const kit = interaction.options.getString('kit');

      if (tier != null && tier.trim() !== '') {
        await interaction.deferReply({ flags: MessageFlags.Ephemeral });
        return applyTierSet(interaction, user, tier.trim(), kit);
      }

      const kitInfo = kit ? VALID_KITS.find(k => k.value === kit.toLowerCase()) : null;
      const row = db.getUserTier(interaction.guildId, user.id, kitInfo?.value || null);
      if (!row) {
        return interaction.reply({ content: `**${user.username}** has no tier result set${kitInfo ? ` for ${kitInfo.name}` : ''}.`, flags: MessageFlags.Ephemeral });
      }
      const modeInfo = VALID_KITS.find(k => k.value === row.mode);
      return interaction.reply({
        content: `**${user.username}** – ${modeInfo?.name || row.mode}: **${row.tier}** (set by <@${row.updated_by}>)`,
        flags: MessageFlags.Ephemeral,
      });
    }

    if (name === 'tiers') {
      const user = interaction.options.getUser('user');
      await interaction.deferReply({ flags: MessageFlags.Ephemeral });

      // Fetch tiers from website API
      const siteUrl = process.env.SITE_SYNC_WEBHOOK_URL?.replace('/api/webhooks/discord-tier', '') || 'http://localhost:3005';
      try {
        const response = await fetch(`${siteUrl}/api/player/${user.id}`);

        if (response.status === 404) {
          return interaction.editReply({ content: `**${user.username}** has no tier results on the website.` });
        }

        if (!response.ok) {
          return interaction.editReply({ content: 'Failed to fetch tiers from website.' });
        }

        const data = await response.json();

        if (!data.tiers || data.tiers.length === 0) {
          return interaction.editReply({ content: `**${user.username}** has no tier results.` });
        }

        const avatarUrl = user.displayAvatarURL({ size: 256 });
        const embed = new EmbedBuilder()
          .setTitle(`${data.player.username}'s Tiers`)
          .setColor(0x5865f2)
          .setThumbnail(avatarUrl)
          .setFooter({ text: `Region: ${data.player.region || 'Unknown'}` })
          .setTimestamp();

        // Add each tier as a field
        for (const t of data.tiers) {
          const kitInfo = VALID_KITS.find(k => k.value === t.mode);
          const kitName = kitInfo?.name || t.mode;
          const testedAt = t.last_tested_at ? new Date(t.last_tested_at).toLocaleDateString() : 'Unknown';
          embed.addFields({
            name: kitName,
            value: `**${t.tier}**\nTester: ${t.tester_name || 'Unknown'}\nDate: ${testedAt}`,
            inline: true,
          });
        }

        return interaction.editReply({ embeds: [embed] });
      } catch (err) {
        console.error('Tiers fetch error:', err);
        return interaction.editReply({ content: 'Failed to fetch tiers. Is the website running?' });
      }
    }

    if (name === 'admin') {
      const sub = interaction.options.getSubcommand();
      if (sub === 'tester-role') {
        const role = interaction.options.getRole('role');
        db.setTesterConfig(interaction.guildId, { tester_role_id: role?.id || null });
        return interaction.reply({
          content: role ? `Tester role set to ${role}. Anyone with this role can use /result.` : 'Tester role cleared. Only users in the tester list can use /result.',
          flags: MessageFlags.Ephemeral,
        });
      }
      if (sub === 'results-channel') {
        const channel = interaction.options.getChannel('channel');
        db.setTesterConfig(interaction.guildId, { results_channel_id: channel.id });
        return interaction.reply({
          content: `Results channel set to ${channel}. Tier results from /result will be posted there.`,
          flags: MessageFlags.Ephemeral,
        });
      }
      if (sub === 'info') {
        const msg = [
          '**METiers — Admin**',
          'Configure who can set tier results and where they are posted.',
          '',
          '• **tester-role** — Role that can use `/result` (e.g. Tier Tester).',
          '• **results-channel** — Channel where tier results are posted (old/new tier, user, tester).',
          '• **config** — Show current tester role, results channel, and tester list.',
          '• **clear** — Clear a user\'s tier result.',
          '• **setkit** — Add a kit/mode for tier testing.',
          '• **removekit** — Remove a kit from the list.',
          '• **kits** — List all configured kits.',
          '',
          `Allowed tiers: **${ALLOWED_TIERS.join(', ')}**`,
        ].join('\n');
        return interaction.reply({ content: msg, flags: MessageFlags.Ephemeral });
      }
      if (sub === 'config') {
        const config = db.getTesterConfig(interaction.guildId);
        const testers = db.getTesters(interaction.guildId);
        const kits = db.getGuildKits(interaction.guildId);
        let text = '**METiers — Tester config**\n';
        text += config?.tester_role_id ? `Tester role: <@&${config.tester_role_id}>\n` : 'Tester role: (not set)\n';
        text += config?.results_channel_id ? `Results channel: <#${config.results_channel_id}>\n` : 'Results channel: (not set)\n';
        text += `Allowed tiers: ${ALLOWED_TIERS.join(', ')}\n`;
        text += `Testers (list): ${testers.length} user(s)\n`;
        text += `Kits: ${kits.length > 0 ? kits.map(k => k.kit_label).join(', ') : '(none configured)'}`;
        return interaction.reply({ content: text, flags: MessageFlags.Ephemeral });
      }
      if (sub === 'clear') {
        const user = interaction.options.getUser('user');
        const had = db.getUserTier(interaction.guildId, user.id);
        db.deleteUserTier(interaction.guildId, user.id);
        return interaction.reply({
          content: had ? `Cleared tier for **${user.username}** (was **${had.tier}**).` : `**${user.username}** had no tier set.`,
          flags: MessageFlags.Ephemeral,
        });
      }
      if (sub === 'setkit') {
        const name = interaction.options.getString('name').toLowerCase().replace(/\s+/g, '_');
        const label = interaction.options.getString('label');
        const emoji = interaction.options.getString('emoji');
        db.addGuildKit(interaction.guildId, name, label, emoji);
        return interaction.reply({
          content: `Kit **${label}** (${name}) added.${emoji ? ` Emoji: ${emoji}` : ''}`,
          flags: MessageFlags.Ephemeral,
        });
      }
      if (sub === 'removekit') {
        const name = interaction.options.getString('name').toLowerCase();
        const result = db.removeGuildKit(interaction.guildId, name);
        if (result.changes === 0) {
          return interaction.reply({ content: `Kit **${name}** not found.`, flags: MessageFlags.Ephemeral });
        }
        return interaction.reply({ content: `Kit **${name}** removed.`, flags: MessageFlags.Ephemeral });
      }
      if (sub === 'kits') {
        const kits = db.getGuildKits(interaction.guildId);
        if (kits.length === 0) {
          return interaction.reply({
            content: 'No kits configured. Use `/admin setkit` to add kits (e.g. sword, axe, netherite_pot).',
            flags: MessageFlags.Ephemeral,
          });
        }
        const lines = kits.map((k, i) => `${i + 1}. ${k.emoji || ''} **${k.kit_label}** (\`${k.kit_name}\`)`);
        return interaction.reply({
          content: `**Configured kits:**\n${lines.join('\n')}`,
          flags: MessageFlags.Ephemeral,
        });
      }
    }
    } catch (err) {
      console.error('Tester/result/admin command error:', err);
      try {
        if (interaction.replied || interaction.deferred) await interaction.editReply({ content: 'Something went wrong. Try again.' }).catch(() => {});
        else await interaction.reply({ content: 'Something went wrong. Try again.', flags: MessageFlags.Ephemeral }).catch(() => {});
      } catch (_) {}
    }
  });
}

module.exports = { getCommands, register };
