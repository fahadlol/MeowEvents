const { SlashCommandBuilder, PermissionFlagsBits, ChannelType, EmbedBuilder, MessageFlags } = require('discord.js');
const db = require('./db');
const panels = require('./panels');

const BUTTON_STYLES = ['Primary', 'Secondary', 'Success', 'Danger'];
const QUESTION_STYLES = ['Short', 'Paragraph'];

function getCommands() {
  const help = new SlashCommandBuilder()
    .setName('help')
    .setDescription('METiers: commands and links');

  // /tickettype command
  const tickettype = new SlashCommandBuilder()
    .setName('tickettype')
    .setDescription('Manage ticket types for panels')
    .setDefaultMemberPermissions(PermissionFlagsBits.ManageGuild)
    .addSubcommand((sub) =>
      sub
        .setName('create')
        .setDescription('Create a ticket type for a panel')
        .addIntegerOption((o) => o.setName('panel_id').setDescription('Panel ID to add ticket type to').setRequired(true))
        .addStringOption((o) => o.setName('name').setDescription('Ticket type name (button label)').setRequired(true).setMaxLength(80))
        .addStringOption((o) => o.setName('emoji').setDescription('Button emoji').setRequired(false))
        .addStringOption((o) => o.setName('style').setDescription('Button style').setRequired(false).addChoices(
          { name: 'Primary (Blue)', value: 'Primary' },
          { name: 'Secondary (Gray)', value: 'Secondary' },
          { name: 'Success (Green)', value: 'Success' },
          { name: 'Danger (Red)', value: 'Danger' }
        ))
        .addChannelOption((o) => o.setName('category').setDescription('Category for this ticket type').addChannelTypes(ChannelType.GuildCategory).setRequired(false))
        .addRoleOption((o) => o.setName('staff_role').setDescription('Staff role for this ticket type').setRequired(false))
    )
    .addSubcommand((sub) =>
      sub
        .setName('edit')
        .setDescription('Edit a ticket type')
        .addIntegerOption((o) => o.setName('type_id').setDescription('Ticket type ID').setRequired(true))
        .addStringOption((o) => o.setName('name').setDescription('Ticket type name').setMaxLength(80).setRequired(false))
        .addStringOption((o) => o.setName('emoji').setDescription('Button emoji (use "none" to remove)').setRequired(false))
        .addStringOption((o) => o.setName('style').setDescription('Button style').setRequired(false).addChoices(
          { name: 'Primary (Blue)', value: 'Primary' },
          { name: 'Secondary (Gray)', value: 'Secondary' },
          { name: 'Success (Green)', value: 'Success' },
          { name: 'Danger (Red)', value: 'Danger' }
        ))
        .addChannelOption((o) => o.setName('category').setDescription('Category for this ticket type').addChannelTypes(ChannelType.GuildCategory).setRequired(false))
        .addRoleOption((o) => o.setName('staff_role').setDescription('Staff role for this ticket type').setRequired(false))
        .addStringOption((o) => o.setName('naming_format').setDescription('Channel naming format ({type}, {number}, {user})').setRequired(false))
        .addIntegerOption((o) => o.setName('delete_delay').setDescription('Seconds before ticket deletion after close').setMinValue(0).setMaxValue(300).setRequired(false))
        .addBooleanOption((o) => o.setName('dm_transcript').setDescription('DM transcript to user on close').setRequired(false))
        .addBooleanOption((o) => o.setName('allow_duplicate').setDescription('Allow multiple open tickets of this type').setRequired(false))
        .addChannelOption((o) => o.setName('log_channel').setDescription('Log channel for this ticket type').addChannelTypes(ChannelType.GuildText).setRequired(false))
        .addStringOption((o) => o.setName('welcome_message').setDescription('Custom welcome message').setRequired(false))
    )
    .addSubcommand((sub) =>
      sub
        .setName('delete')
        .setDescription('Delete a ticket type')
        .addIntegerOption((o) => o.setName('type_id').setDescription('Ticket type ID').setRequired(true))
    )
    .addSubcommand((sub) =>
      sub
        .setName('list')
        .setDescription('List ticket types for a panel')
        .addIntegerOption((o) => o.setName('panel_id').setDescription('Panel ID').setRequired(true))
    )
    .addSubcommandGroup((group) =>
      group
        .setName('questions')
        .setDescription('Manage questions for a ticket type')
        .addSubcommand((sub) =>
          sub
            .setName('add')
            .setDescription('Add a question to a ticket type')
            .addIntegerOption((o) => o.setName('type_id').setDescription('Ticket type ID').setRequired(true))
            .addStringOption((o) => o.setName('label').setDescription('Question label').setRequired(true).setMaxLength(45))
            .addStringOption((o) => o.setName('placeholder').setDescription('Input placeholder text').setMaxLength(100).setRequired(false))
            .addStringOption((o) => o.setName('style').setDescription('Input style').setRequired(false).addChoices(
              { name: 'Short (single line)', value: 'Short' },
              { name: 'Paragraph (multi-line)', value: 'Paragraph' }
            ))
            .addBooleanOption((o) => o.setName('required').setDescription('Is this field required?').setRequired(false))
        )
        .addSubcommand((sub) =>
          sub
            .setName('remove')
            .setDescription('Remove a question')
            .addIntegerOption((o) => o.setName('question_id').setDescription('Question ID').setRequired(true))
        )
        .addSubcommand((sub) =>
          sub
            .setName('list')
            .setDescription('List questions for a ticket type')
            .addIntegerOption((o) => o.setName('type_id').setDescription('Ticket type ID').setRequired(true))
        )
    );

  // /config command
  const config = new SlashCommandBuilder()
    .setName('config')
    .setDescription('Guild configuration')
    .setDefaultMemberPermissions(PermissionFlagsBits.ManageGuild)
    .addSubcommand((sub) =>
      sub
        .setName('logchannel')
        .setDescription('Set default log channel for guild')
        .addChannelOption((o) => o.setName('channel').setDescription('Log channel').addChannelTypes(ChannelType.GuildText).setRequired(true))
    )
    .addSubcommand((sub) =>
      sub.setName('view').setDescription('View current guild configuration')
    );

  const create = new SlashCommandBuilder()
    .setName('panel')
    .setDescription('Manage ticket panels')
    .setDefaultMemberPermissions(PermissionFlagsBits.ManageGuild)
    .addSubcommand((sub) =>
      sub
        .setName('create')
        .setDescription('Create a ticket panel in this channel')
        .addRoleOption((o) => o.setName('role').setDescription('Role to ping when a ticket opens').setRequired(true))
        .addChannelOption((o) =>
          o.setName('channel').setDescription('Channel for the panel (default: current)').setRequired(false).addChannelTypes(ChannelType.GuildText)
        )
        .addChannelOption((o) =>
          o.setName('category').setDescription('Category for ticket channels').setRequired(false).addChannelTypes(ChannelType.GuildCategory)
        )
        .addStringOption((o) => o.setName('title').setDescription('Panel title').setRequired(false))
        .addStringOption((o) => o.setName('description').setDescription('Panel description').setRequired(false))
        .addStringOption((o) => o.setName('button').setDescription('Button label').setRequired(false))
        .addChannelOption((o) =>
          o.setName('log').setDescription('Log channel for transcripts and events').setRequired(false).addChannelTypes(ChannelType.GuildText)
        )
        .addIntegerOption((o) => o.setName('auto_close_days').setDescription('Auto-close tickets after N days of no messages (0 = off)').setRequired(false).setMinValue(0).setMaxValue(365))
    )
    .addSubcommand((sub) =>
      sub.setName('list').setDescription('List ticket panels in this server')
    )
    .addSubcommand((sub) =>
      sub
        .setName('delete')
        .setDescription('Delete a ticket panel')
        .addIntegerOption((o) => o.setName('id').setDescription('Panel ID (use /panel list to see IDs)').setRequired(true))
    )
    .addSubcommand((sub) =>
      sub
        .setName('disable')
        .setDescription('Temporarily disable new tickets for a panel')
        .addStringOption((o) => o.setName('name').setDescription('Panel title (exact match)').setRequired(true))
    )
    .addSubcommand((sub) =>
      sub
        .setName('enable')
        .setDescription('Re-enable new tickets for a panel')
        .addStringOption((o) => o.setName('name').setDescription('Panel title (exact match)').setRequired(true))
    );

  const addCmd = new SlashCommandBuilder()
    .setName('add')
    .setDescription('Add a user to this ticket (use in a ticket channel)')
    .addUserOption((o) => o.setName('user').setDescription('User to add').setRequired(true));

  const removeCmd = new SlashCommandBuilder()
    .setName('remove')
    .setDescription('Remove a user from this ticket (use in a ticket channel)')
    .addUserOption((o) => o.setName('user').setDescription('User to remove').setRequired(true));

  const renameCmd = new SlashCommandBuilder()
    .setName('rename')
    .setDescription('Rename this ticket channel (use in a ticket channel)')
    .addStringOption((o) => o.setName('name').setDescription('New channel name (lowercase, no spaces)').setRequired(true));

  const closeCmd = new SlashCommandBuilder()
    .setName('close')
    .setDescription('Close this ticket (use in a ticket channel)')
    .addSubcommand((sub) =>
      sub
        .setName('now')
        .setDescription('Close ticket after 3 seconds')
        .addStringOption((o) => o.setName('reason').setDescription('Optional reason').setRequired(false))
    )
    .addSubcommand((sub) =>
      sub
        .setName('request')
        .setDescription('Request ticket opener to accept or deny close')
        .addStringOption((o) => o.setName('reason').setDescription('Reason for closing').setRequired(false))
        .addIntegerOption((o) => o.setName('duration').setDescription('Seconds to wait for response (auto-close if no response)').setRequired(false).setMinValue(10).setMaxValue(600))
    );

  return [help.toJSON(), create.toJSON(), tickettype.toJSON(), config.toJSON(), addCmd.toJSON(), removeCmd.toJSON(), renameCmd.toJSON(), closeCmd.toJSON()];
}

function register(client) {
  client.on('interactionCreate', async (interaction) => {
    if (!interaction.isChatInputCommand()) return;

    try {
    if (interaction.commandName === 'help') {
      const inviteUrl = process.env.NEXT_PUBLIC_DISCORD_INVITE_URL || process.env.DISCORD_INVITE_URL;
      const siteUrl = process.env.NEXT_PUBLIC_SITE_URL;
      const embed = new EmbedBuilder()
        .setTitle('METiers')
        .setColor(0x5865f2)
        .setDescription('Ticket panels + tier results for PvP servers.')
        .addFields(
          { name: 'Tickets', value: '`/panel create` — Create a ticket panel. `/panel list` · `/panel delete`', inline: false },
          { name: 'Tiers', value: '`/result user [tier]` — Set or view tier (LT5, HT5, HT4, LT4, LT3). `/set user tier`', inline: false },
          { name: 'Admin', value: '`/admin results-channel` — Where results post. `/admin tester-role` · `/admin config` · `/admin clear user`', inline: false }
        )
        .setFooter({ text: 'Use /admin info for full admin options' })
        .setTimestamp();
      if (inviteUrl) embed.addFields({ name: 'Discord', value: inviteUrl, inline: false });
      if (siteUrl) embed.addFields({ name: 'Leaderboard', value: siteUrl, inline: false });
      return interaction.reply({ embeds: [embed], flags: MessageFlags.Ephemeral });
    }

    if (interaction.commandName !== 'panel') return;

    const sub = interaction.options.getSubcommand();
    if (sub === 'list') {
      const list = db.getPanelsByGuild(interaction.guildId);
      if (list.length === 0) {
        return interaction.reply({ content: 'No ticket panels in this server. Use `/panel create` to add one.', flags: MessageFlags.Ephemeral });
      }
      const lines = list.map(
        (p) => `**${p.title}** (ID: ${p.id}) – <#${p.channel_id}> · ${db.getOpenTicketCount(p.id)} open${p.disabled ? ' · **disabled**' : ''}`
      );
      return interaction.reply({
        content: `**Ticket panels** (${list.length}/${db.MAX_PANELS}):\n${lines.join('\n')}`,
        flags: MessageFlags.Ephemeral,
      });
    }

    if (sub === 'disable') {
      const name = interaction.options.getString('name');
      const panel = db.getPanelByTitle(interaction.guildId, name);
      if (!panel) {
        return interaction.reply({ content: `No panel found with title **${name}**. Use \`/panel list\` to see exact titles.`, flags: MessageFlags.Ephemeral });
      }
      await interaction.deferReply({ flags: MessageFlags.Ephemeral });
      const updated = db.setPanelDisabled(panel.id, true);
      await panels.updatePanelMessage(client, updated);
      return interaction.editReply({ content: `Panel **${updated.title}** (ID ${updated.id}) is now disabled. New tickets cannot be opened.` });
    }

    if (sub === 'enable') {
      const name = interaction.options.getString('name');
      const panel = db.getPanelByTitle(interaction.guildId, name);
      if (!panel) {
        return interaction.reply({ content: `No panel found with title **${name}**. Use \`/panel list\` to see exact titles.`, flags: MessageFlags.Ephemeral });
      }
      await interaction.deferReply({ flags: MessageFlags.Ephemeral });
      db.setPanelDisabled(panel.id, false);
      const updated = db.getPanel(panel.id);
      await panels.updatePanelMessage(client, updated);
      return interaction.editReply({ content: `Panel **${updated.title}** (ID ${updated.id}) is now enabled.` });
    }

    if (sub === 'delete') {
      const id = interaction.options.getInteger('id');
      const panel = db.getPanel(id);
      if (!panel || panel.guild_id !== interaction.guildId) {
        return interaction.reply({ content: 'Panel not found or not in this server.', flags: MessageFlags.Ephemeral });
      }
      await interaction.deferReply({ flags: MessageFlags.Ephemeral });
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
        return interaction.editReply({ content: `Panel **${panel.title}** (ID ${id}) deleted.` });
      } catch (err) {
        console.error('Panel delete error:', err);
        return interaction.editReply({ content: `Failed: ${err.message}` });
      }
    }

    if (sub === 'create') {
      const guildId = interaction.guildId;
      if (db.getPanelCount(guildId) >= db.MAX_PANELS) {
        return interaction.reply({
          content: `Maximum ${db.MAX_PANELS} panels per server.`,
          flags: MessageFlags.Ephemeral,
        });
      }
      await interaction.deferReply({ flags: MessageFlags.Ephemeral });
      const channel = interaction.options.getChannel('channel') || interaction.channel;
      const role = interaction.options.getRole('role');
      const category = interaction.options.getChannel('category');
      const title = interaction.options.getString('title') || 'Support Tickets';
      const description = interaction.options.getString('description') || 'Click the button below to open a ticket.';
      const buttonLabel = interaction.options.getString('button') || 'Open Ticket';
      const logChannel = interaction.options.getChannel('log');
      const autoCloseDays = interaction.options.getInteger('auto_close_days');

      const panelData = {
        guild_id: guildId,
        channel_id: channel.id,
        role_id: role.id,
        category_id: category?.id || null,
        title,
        description,
        button_label: buttonLabel,
        button_emoji: null,
        custom_message: null,
        log_channel_id: logChannel?.id || null,
        auto_close_days: autoCloseDays != null && autoCloseDays > 0 ? autoCloseDays : null,
      };

      try {
        const id = db.createPanel({ ...panelData, message_id: null });
        const messageId = await panels.postPanel(channel, { ...panelData, id });
        db.updatePanelMessage(id, messageId);
        await interaction.editReply({
          content: `Ticket panel created in ${channel}. Use \`/tickettype create\` to add ticket types, or use the dashboard.`,
        });
      } catch (err) {
        console.error('Panel create error:', err);
        await interaction.editReply({ content: `Failed: ${err.message}` });
      }
    }
    } catch (err) {
      console.error('Panel/help command error:', err);
      try {
        if (interaction.replied || interaction.deferred) await interaction.editReply({ content: 'Something went wrong. Try again.' }).catch(() => {});
        else await interaction.reply({ content: 'Something went wrong. Try again.', flags: MessageFlags.Ephemeral }).catch(() => {});
      } catch (_) {}
    }
  });

  // ----- /tickettype command handler -----
  client.on('interactionCreate', async (interaction) => {
    if (!interaction.isChatInputCommand()) return;
    if (interaction.commandName !== 'tickettype') return;

    try {
      const subGroup = interaction.options.getSubcommandGroup(false);
      const sub = interaction.options.getSubcommand();

      // Handle questions subcommand group
      if (subGroup === 'questions') {
        if (sub === 'add') {
          const typeId = interaction.options.getInteger('type_id');
          const ticketType = db.getTicketType(typeId);
          if (!ticketType) {
            return interaction.reply({ content: 'Ticket type not found.', flags: MessageFlags.Ephemeral });
          }
          const panel = db.getPanel(ticketType.panel_id);
          if (!panel || panel.guild_id !== interaction.guildId) {
            return interaction.reply({ content: 'Ticket type not found in this server.', flags: MessageFlags.Ephemeral });
          }

          const questionCount = db.getQuestionCount(typeId);
          if (questionCount >= 5) {
            return interaction.reply({ content: 'Maximum 5 questions per ticket type (Discord modal limit).', flags: MessageFlags.Ephemeral });
          }

          const label = interaction.options.getString('label');
          const placeholder = interaction.options.getString('placeholder');
          const style = interaction.options.getString('style') || 'Short';
          const required = interaction.options.getBoolean('required') !== false;

          const questionId = db.createQuestion(typeId, {
            question_label: label,
            question_placeholder: placeholder,
            question_style: style,
            required,
          });

          return interaction.reply({
            content: `Question added (ID: ${questionId}): **${label}**`,
            flags: MessageFlags.Ephemeral,
          });
        }

        if (sub === 'remove') {
          const questionId = interaction.options.getInteger('question_id');
          const question = db.getQuestion(questionId);
          if (!question) {
            return interaction.reply({ content: 'Question not found.', flags: MessageFlags.Ephemeral });
          }
          const ticketType = db.getTicketType(question.ticket_type_id);
          const panel = ticketType ? db.getPanel(ticketType.panel_id) : null;
          if (!panel || panel.guild_id !== interaction.guildId) {
            return interaction.reply({ content: 'Question not found in this server.', flags: MessageFlags.Ephemeral });
          }

          db.deleteQuestion(questionId);
          return interaction.reply({
            content: `Question **${question.question_label}** deleted.`,
            flags: MessageFlags.Ephemeral,
          });
        }

        if (sub === 'list') {
          const typeId = interaction.options.getInteger('type_id');
          const ticketType = db.getTicketType(typeId);
          if (!ticketType) {
            return interaction.reply({ content: 'Ticket type not found.', flags: MessageFlags.Ephemeral });
          }
          const panel = db.getPanel(ticketType.panel_id);
          if (!panel || panel.guild_id !== interaction.guildId) {
            return interaction.reply({ content: 'Ticket type not found in this server.', flags: MessageFlags.Ephemeral });
          }

          const questions = db.getQuestionsByType(typeId);
          if (questions.length === 0) {
            return interaction.reply({
              content: `**${ticketType.name}** has no questions. Users can open tickets without filling a form.`,
              flags: MessageFlags.Ephemeral,
            });
          }

          const lines = questions.map((q, i) =>
            `${i + 1}. **${q.question_label}** (ID: ${q.id}) - ${q.question_style}${q.required ? ' *required*' : ''}`
          );
          return interaction.reply({
            content: `**Questions for ${ticketType.name}** (${questions.length}/5):\n${lines.join('\n')}`,
            flags: MessageFlags.Ephemeral,
          });
        }
        return;
      }

      // Main tickettype subcommands
      if (sub === 'create') {
        const panelId = interaction.options.getInteger('panel_id');
        const panel = db.getPanel(panelId);
        if (!panel || panel.guild_id !== interaction.guildId) {
          return interaction.reply({ content: 'Panel not found in this server.', flags: MessageFlags.Ephemeral });
        }

        const typeCount = db.getTicketTypeCount(panelId);
        if (typeCount >= 5) {
          return interaction.reply({ content: 'Maximum 5 ticket types per panel (Discord button limit).', flags: MessageFlags.Ephemeral });
        }

        const name = interaction.options.getString('name');
        const emoji = interaction.options.getString('emoji');
        const style = interaction.options.getString('style') || 'Primary';
        const category = interaction.options.getChannel('category');
        const staffRole = interaction.options.getRole('staff_role');

        const typeId = db.createTicketType({
          panel_id: panelId,
          name,
          emoji,
          button_style: style,
          category_id: category?.id || panel.category_id,
          staff_role_ids: staffRole ? [staffRole.id] : [panel.role_id],
          log_channel_id: panel.log_channel_id,
        });

        // Update panel message to show new buttons
        await panels.updatePanelMessage(client, panel);

        return interaction.reply({
          content: `Ticket type **${name}** created (ID: ${typeId}). Use \`/tickettype questions add\` to add form questions.`,
          flags: MessageFlags.Ephemeral,
        });
      }

      if (sub === 'edit') {
        const typeId = interaction.options.getInteger('type_id');
        const ticketType = db.getTicketType(typeId);
        if (!ticketType) {
          return interaction.reply({ content: 'Ticket type not found.', flags: MessageFlags.Ephemeral });
        }
        const panel = db.getPanel(ticketType.panel_id);
        if (!panel || panel.guild_id !== interaction.guildId) {
          return interaction.reply({ content: 'Ticket type not found in this server.', flags: MessageFlags.Ephemeral });
        }

        const updates = {};
        const name = interaction.options.getString('name');
        const emoji = interaction.options.getString('emoji');
        const style = interaction.options.getString('style');
        const category = interaction.options.getChannel('category');
        const staffRole = interaction.options.getRole('staff_role');
        const namingFormat = interaction.options.getString('naming_format');
        const deleteDelay = interaction.options.getInteger('delete_delay');
        const dmTranscript = interaction.options.getBoolean('dm_transcript');
        const allowDuplicate = interaction.options.getBoolean('allow_duplicate');
        const logChannel = interaction.options.getChannel('log_channel');
        const welcomeMessage = interaction.options.getString('welcome_message');

        if (name) updates.name = name;
        if (emoji !== null) updates.emoji = emoji === 'none' ? null : emoji;
        if (style) updates.button_style = style;
        if (category) updates.category_id = category.id;
        if (staffRole) updates.staff_role_ids = [staffRole.id];
        if (namingFormat) updates.naming_format = namingFormat;
        if (deleteDelay !== null) updates.delete_delay_seconds = deleteDelay;
        if (dmTranscript !== null) updates.dm_transcript = dmTranscript;
        if (allowDuplicate !== null) updates.allow_duplicate = allowDuplicate;
        if (logChannel) updates.log_channel_id = logChannel.id;
        if (welcomeMessage) updates.welcome_message = welcomeMessage;

        if (Object.keys(updates).length === 0) {
          return interaction.reply({ content: 'No changes specified.', flags: MessageFlags.Ephemeral });
        }

        db.updateTicketType(typeId, updates);
        await panels.updatePanelMessage(client, panel);

        return interaction.reply({
          content: `Ticket type **${name || ticketType.name}** updated.`,
          flags: MessageFlags.Ephemeral,
        });
      }

      if (sub === 'delete') {
        const typeId = interaction.options.getInteger('type_id');
        const ticketType = db.getTicketType(typeId);
        if (!ticketType) {
          return interaction.reply({ content: 'Ticket type not found.', flags: MessageFlags.Ephemeral });
        }
        const panel = db.getPanel(ticketType.panel_id);
        if (!panel || panel.guild_id !== interaction.guildId) {
          return interaction.reply({ content: 'Ticket type not found in this server.', flags: MessageFlags.Ephemeral });
        }

        const typeCount = db.getTicketTypeCount(panel.id);
        if (typeCount <= 1) {
          return interaction.reply({
            content: 'Cannot delete the last ticket type. Panels must have at least one ticket type.',
            flags: MessageFlags.Ephemeral,
          });
        }

        db.deleteTicketType(typeId);
        await panels.updatePanelMessage(client, panel);

        return interaction.reply({
          content: `Ticket type **${ticketType.name}** deleted.`,
          flags: MessageFlags.Ephemeral,
        });
      }

      if (sub === 'list') {
        const panelId = interaction.options.getInteger('panel_id');
        const panel = db.getPanel(panelId);
        if (!panel || panel.guild_id !== interaction.guildId) {
          return interaction.reply({ content: 'Panel not found in this server.', flags: MessageFlags.Ephemeral });
        }

        const types = db.getTicketTypesByPanel(panelId);
        if (types.length === 0) {
          return interaction.reply({
            content: `Panel **${panel.title}** has no ticket types. Use \`/tickettype create\` to add one.`,
            flags: MessageFlags.Ephemeral,
          });
        }

        const lines = types.map((t) => {
          const questionCount = db.getQuestionCount(t.id);
          return `• **${t.name}** (ID: ${t.id}) - ${t.button_style}${t.emoji ? ` ${t.emoji}` : ''} - ${questionCount} questions`;
        });

        return interaction.reply({
          content: `**Ticket types for ${panel.title}** (${types.length}/5):\n${lines.join('\n')}`,
          flags: MessageFlags.Ephemeral,
        });
      }
    } catch (err) {
      console.error('Tickettype command error:', err);
      try {
        if (interaction.replied || interaction.deferred) await interaction.editReply({ content: 'Something went wrong. Try again.' }).catch(() => {});
        else await interaction.reply({ content: 'Something went wrong. Try again.', flags: MessageFlags.Ephemeral }).catch(() => {});
      } catch (_) {}
    }
  });

  // ----- /config command handler -----
  client.on('interactionCreate', async (interaction) => {
    if (!interaction.isChatInputCommand()) return;
    if (interaction.commandName !== 'config') return;

    try {
      const sub = interaction.options.getSubcommand();

      if (sub === 'logchannel') {
        const channel = interaction.options.getChannel('channel');
        db.setGuildConfig(interaction.guildId, { default_log_channel_id: channel.id });
        return interaction.reply({
          content: `Default log channel set to ${channel}.`,
          flags: MessageFlags.Ephemeral,
        });
      }

      if (sub === 'view') {
        const config = db.getGuildConfig(interaction.guildId);
        const panelCount = db.getPanelCount(interaction.guildId);

        const embed = new EmbedBuilder()
          .setTitle('Guild Configuration')
          .setColor(0x5865f2)
          .addFields(
            { name: 'Panels', value: `${panelCount}/${db.MAX_PANELS}`, inline: true },
            { name: 'Default Log Channel', value: config?.default_log_channel_id ? `<#${config.default_log_channel_id}>` : 'Not set', inline: true }
          )
          .setTimestamp();

        return interaction.reply({ embeds: [embed], flags: MessageFlags.Ephemeral });
      }
    } catch (err) {
      console.error('Config command error:', err);
      try {
        if (interaction.replied || interaction.deferred) await interaction.editReply({ content: 'Something went wrong. Try again.' }).catch(() => {});
        else await interaction.reply({ content: 'Something went wrong. Try again.', flags: MessageFlags.Ephemeral }).catch(() => {});
      } catch (_) {}
    }
  });
}

module.exports = { getCommands, register };
