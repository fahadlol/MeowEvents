const { SlashCommandBuilder, EmbedBuilder, MessageFlags } = require('discord.js');

const SITE_URL = process.env.SITE_URL || process.env.NEXT_PUBLIC_SITE_URL || '';
const MODE_NAMES = {
  sword: 'Sword',
  axe: 'Axe',
  netherite_pot: 'Netherite Pot',
  diamond_pot: 'Diamond Pot',
  vanilla: 'Vanilla',
  mace: 'Mace',
  uhc: 'UHC',
  smp: 'SMP',
};

function getCommands() {
  const profile = new SlashCommandBuilder()
    .setName('profile')
    .setDescription('View METiers profile / rank (yours or another user)')
    .addUserOption((o) => o.setName('user').setDescription('User to look up (must be linked on site)').setRequired(false));

  const leaderboard = new SlashCommandBuilder()
    .setName('leaderboard')
    .setDescription('Quick METiers leaderboard (top 10)')
    .addStringOption((o) =>
      o.setName('type')
        .setDescription('Overall or a specific mode')
        .setRequired(true)
        .addChoices(
          { name: 'Overall', value: 'overall' },
          { name: 'Sword', value: 'sword' },
          { name: 'Axe', value: 'axe' },
          { name: 'Netherite Pot', value: 'netherite_pot' },
          { name: 'Diamond Pot', value: 'diamond_pot' },
          { name: 'Vanilla', value: 'vanilla' },
          { name: 'Mace', value: 'mace' },
          { name: 'UHC', value: 'uhc' },
          { name: 'SMP', value: 'smp' }
        )
    );

  return [profile.toJSON(), leaderboard.toJSON()];
}

async function fetchSite(path) {
  if (!SITE_URL) return null;
  const url = `${SITE_URL.replace(/\/$/, '')}${path}`;
  const res = await fetch(url, { headers: { Accept: 'application/json' } }).catch(() => null);
  if (!res || !res.ok) return null;
  return res.json();
}

function register(client) {
  client.on('interactionCreate', async (interaction) => {
    if (!interaction.isChatInputCommand()) return;
    const name = interaction.commandName;
    if (name !== 'profile' && name !== 'leaderboard') return;

    if (!SITE_URL) {
      return interaction.reply({
        content: 'Site URL not configured (SITE_URL). Profile and leaderboard commands are disabled.',
        flags: MessageFlags.Ephemeral,
      }).catch(() => {});
    }

    try {
      if (name === 'profile') {
        const user = interaction.options.getUser('user') || interaction.user;
        await interaction.deferReply({ flags: MessageFlags.Ephemeral });
        const data = await fetchSite(`/api/player/${user.id}`);
        if (!data || !data.player) {
          return interaction.editReply({
            content: user.id === interaction.user.id
              ? 'Your Discord account is not linked to a METiers profile. Link it in Admin on the site.'
              : 'That user is not linked to a METiers profile.',
          });
        }
        const p = data.player;
        const rankStr = p.overall_rank != null && p.total_players != null
          ? `**#${p.overall_rank}** of ${p.total_players}`
          : '—';
        const pointsStr = p.total_points != null ? `${p.total_points} points` : '—';
        const tierLines = (data.tiers || []).map((t) => `**${MODE_NAMES[t.mode] || t.mode}:** ${t.tier}`).join('\n');
        const embed = new EmbedBuilder()
          .setColor(0x5865f2)
          .setTitle(`${p.username} | METiers`)
          .setDescription(p.region || '')
          .addFields(
            { name: 'Overall rank', value: rankStr, inline: true },
            { name: 'Points', value: pointsStr, inline: true },
            { name: 'Tiers', value: tierLines || '—', inline: false }
          )
          .setTimestamp();
        if (p.profile_url) embed.setURL(p.profile_url);
        return interaction.editReply({ embeds: [embed] });
      }

      if (name === 'leaderboard') {
        const type = interaction.options.getString('type');
        await interaction.deferReply();
        const path = type === 'overall' ? '/api/leaderboard/overall?limit=10' : `/api/leaderboard/mode/${type}?limit=10`;
        const data = await fetchSite(path);
        if (!data || !data.leaderboard || data.leaderboard.length === 0) {
          return interaction.editReply({ content: 'No leaderboard data available.' });
        }
        const title = type === 'overall' ? 'Overall' : (MODE_NAMES[type] || type);
        const lines = data.leaderboard.map((row, i) => {
          const rank = i + 1;
          if (type === 'overall') {
            return `${rank}. **${row.username}** — ${row.overall_score} pts`;
          }
          return `${rank}. **${row.username}** — ${row.tier}`;
        });
        const embed = new EmbedBuilder()
          .setColor(0x5865f2)
          .setTitle(`${title} — Top 10`)
          .setDescription(lines.join('\n'))
          .setTimestamp();
        const fullUrl = type === 'overall' ? `${SITE_URL}/leaderboards` : `${SITE_URL}/mode/${type}`;
        embed.setURL(fullUrl);
        return interaction.editReply({ embeds: [embed] });
      }
    } catch (err) {
      console.error('Rank command error:', err);
      try {
        if (interaction.deferred) await interaction.editReply({ content: 'Could not fetch data from the site. Try again later.' }).catch(() => {});
        else await interaction.reply({ content: 'Could not fetch data from the site.', flags: MessageFlags.Ephemeral }).catch(() => {});
      } catch (_) {}
    }
  });
}

module.exports = { getCommands, register };
