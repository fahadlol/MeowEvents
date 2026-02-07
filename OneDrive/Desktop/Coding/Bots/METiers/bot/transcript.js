const db = require('./db');

/**
 * Fetches all messages in a channel.
 * @param {TextChannel} channel - Discord channel
 * @returns {Promise<Message[]>}
 */
async function fetchAllMessages(channel) {
  const all = [];
  let lastId;
  while (true) {
    const opts = { limit: 100 };
    if (lastId) opts.before = lastId;
    const messages = await channel.messages.fetch(opts);
    if (messages.size === 0) break;
    all.push(...messages.values());
    lastId = messages.last().id;
    if (messages.size < 100) break;
  }
  return [...all].sort((a, b) => a.createdTimestamp - b.createdTimestamp);
}

/**
 * Fetches all messages in a channel and returns a transcript string.
 * @param {TextChannel} channel - Discord channel
 * @returns {Promise<string>}
 */
async function buildTranscript(channel) {
  const sorted = await fetchAllMessages(channel);
  const lines = sorted.map((msg) => {
    const time = new Date(msg.createdTimestamp).toISOString();
    const author = msg.author?.username || 'Unknown';
    const content = msg.content || '(no text)';
    const text = content.replace(/\n/g, '\n  ');
    let line = `[${time}] ${author}: ${text}`;
    if (msg.attachments.size) {
      msg.attachments.forEach((a) => { line += `\n  [Attachment] ${a.url}`; });
    }
    return line;
  });
  return lines.join('\n') || '(no messages)';
}

/**
 * Escapes HTML special characters.
 */
function escapeHtml(text) {
  if (!text) return '';
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

/**
 * Formats a timestamp for display.
 */
function formatTimestamp(timestamp) {
  const date = new Date(timestamp);
  return date.toLocaleString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: true,
    timeZone: 'UTC',
    timeZoneName: 'short',
  });
}

/**
 * Converts Discord markdown to HTML.
 */
function markdownToHtml(text) {
  if (!text) return '';
  let html = escapeHtml(text);
  // Bold
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
  // Italic
  html = html.replace(/\*(.+?)\*/g, '<em>$1</em>');
  html = html.replace(/_(.+?)_/g, '<em>$1</em>');
  // Strikethrough
  html = html.replace(/~~(.+?)~~/g, '<del>$1</del>');
  // Code blocks
  html = html.replace(/```(\w*)\n?([\s\S]*?)```/g, '<pre><code>$2</code></pre>');
  // Inline code
  html = html.replace(/`(.+?)`/g, '<code>$1</code>');
  // Line breaks
  html = html.replace(/\n/g, '<br>');
  // User mentions
  html = html.replace(/&lt;@!?(\d+)&gt;/g, '<span class="mention">@User</span>');
  // Role mentions
  html = html.replace(/&lt;@&amp;(\d+)&gt;/g, '<span class="mention">@Role</span>');
  // Channel mentions
  html = html.replace(/&lt;#(\d+)&gt;/g, '<span class="mention">#channel</span>');
  // URLs
  html = html.replace(/(https?:\/\/[^\s<]+)/g, '<a href="$1" target="_blank">$1</a>');
  return html;
}

/**
 * Gets a default avatar URL based on user discriminator.
 */
function getDefaultAvatar(userId) {
  const index = (BigInt(userId) >> 22n) % 6n;
  return `https://cdn.discordapp.com/embed/avatars/${index}.png`;
}

/**
 * Builds an HTML transcript of the ticket.
 * @param {TextChannel} channel - Discord channel
 * @param {Object} ticket - Ticket object from database
 * @returns {Promise<string>} HTML content
 */
async function buildHtmlTranscript(channel, ticket) {
  const messages = await fetchAllMessages(channel);
  const responses = ticket ? db.getTicketResponses(ticket.id) : [];
  const ticketType = ticket?.ticket_type_id ? db.getTicketType(ticket.ticket_type_id) : null;

  const guildName = escapeHtml(channel.guild?.name || 'Unknown Server');
  const channelName = escapeHtml(channel.name || 'Unknown Channel');
  const typeName = escapeHtml(ticketType?.name || 'Ticket');
  const ticketNumber = ticket?.number || '?';
  const createdAt = ticket?.created_at ? formatTimestamp(ticket.created_at * 1000) : 'Unknown';

  // Get ticket owner info
  let ownerName = 'Unknown';
  let ownerAvatar = getDefaultAvatar('0');
  if (ticket?.user_id) {
    try {
      const owner = await channel.guild.members.fetch(ticket.user_id).catch(() => null);
      if (owner) {
        ownerName = escapeHtml(owner.user.username);
        ownerAvatar = owner.user.displayAvatarURL({ format: 'png', size: 64 });
      }
    } catch (e) {}
  }

  // Build HTML
  let html = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>${typeName} #${ticketNumber} - Transcript</title>
  <style>
    * {
      margin: 0;
      padding: 0;
      box-sizing: border-box;
    }
    body {
      font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
      background-color: #36393f;
      color: #dcddde;
      line-height: 1.5;
    }
    .container {
      max-width: 900px;
      margin: 0 auto;
      padding: 20px;
    }
    .header {
      background: linear-gradient(135deg, #5865f2 0%, #7289da 100%);
      padding: 30px;
      border-radius: 8px;
      margin-bottom: 20px;
      color: white;
    }
    .header h1 {
      font-size: 24px;
      margin-bottom: 10px;
    }
    .header-info {
      display: flex;
      flex-wrap: wrap;
      gap: 20px;
      font-size: 14px;
      opacity: 0.9;
    }
    .header-info span {
      display: flex;
      align-items: center;
      gap: 5px;
    }
    .form-responses {
      background-color: #2f3136;
      border-radius: 8px;
      padding: 20px;
      margin-bottom: 20px;
      border-left: 4px solid #5865f2;
    }
    .form-responses h2 {
      font-size: 16px;
      color: #5865f2;
      margin-bottom: 15px;
    }
    .form-field {
      margin-bottom: 15px;
    }
    .form-field:last-child {
      margin-bottom: 0;
    }
    .form-label {
      font-weight: 600;
      color: #b9bbbe;
      font-size: 12px;
      text-transform: uppercase;
      margin-bottom: 5px;
    }
    .form-value {
      color: #dcddde;
      background-color: #40444b;
      padding: 10px;
      border-radius: 4px;
      white-space: pre-wrap;
      word-break: break-word;
    }
    .messages {
      background-color: #36393f;
    }
    .message {
      display: flex;
      padding: 10px 20px;
      border-radius: 4px;
    }
    .message:hover {
      background-color: #32353b;
    }
    .message-avatar {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      margin-right: 15px;
      flex-shrink: 0;
    }
    .message-content {
      flex: 1;
      min-width: 0;
    }
    .message-header {
      display: flex;
      align-items: baseline;
      gap: 8px;
      margin-bottom: 4px;
    }
    .message-author {
      font-weight: 600;
      color: #ffffff;
    }
    .message-author.bot {
      color: #5865f2;
    }
    .bot-tag {
      background-color: #5865f2;
      color: white;
      font-size: 10px;
      padding: 2px 5px;
      border-radius: 3px;
      font-weight: 500;
    }
    .message-time {
      font-size: 12px;
      color: #72767d;
    }
    .message-text {
      color: #dcddde;
      word-break: break-word;
    }
    .message-text a {
      color: #00aff4;
    }
    .message-text code {
      background-color: #2f3136;
      padding: 2px 5px;
      border-radius: 3px;
      font-family: Consolas, Monaco, monospace;
      font-size: 14px;
    }
    .message-text pre {
      background-color: #2f3136;
      padding: 10px;
      border-radius: 4px;
      margin: 10px 0;
      overflow-x: auto;
    }
    .message-text pre code {
      padding: 0;
      background: none;
    }
    .mention {
      background-color: rgba(88, 101, 242, 0.3);
      color: #dee0fc;
      padding: 0 2px;
      border-radius: 3px;
    }
    .attachment {
      margin-top: 10px;
    }
    .attachment img {
      max-width: 400px;
      max-height: 300px;
      border-radius: 4px;
    }
    .attachment-file {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      background-color: #2f3136;
      padding: 10px 15px;
      border-radius: 4px;
      color: #00aff4;
      text-decoration: none;
    }
    .attachment-file:hover {
      text-decoration: underline;
    }
    .embed {
      border-left: 4px solid #5865f2;
      background-color: #2f3136;
      padding: 15px;
      border-radius: 4px;
      margin-top: 10px;
      max-width: 500px;
    }
    .embed-title {
      font-weight: 600;
      color: #ffffff;
      margin-bottom: 8px;
    }
    .embed-description {
      color: #dcddde;
      font-size: 14px;
    }
    .embed-field {
      margin-top: 10px;
    }
    .embed-field-name {
      font-weight: 600;
      color: #ffffff;
      font-size: 14px;
    }
    .embed-field-value {
      color: #dcddde;
      font-size: 14px;
    }
    .footer {
      text-align: center;
      padding: 30px;
      color: #72767d;
      font-size: 12px;
    }
    .divider {
      height: 1px;
      background-color: #40444b;
      margin: 20px 0;
    }
  </style>
</head>
<body>
  <div class="container">
    <div class="header">
      <h1>${typeName} - Ticket #${ticketNumber}</h1>
      <div class="header-info">
        <span>Server: ${guildName}</span>
        <span>Channel: #${channelName}</span>
        <span>Opened: ${createdAt}</span>
        <span>Owner: ${ownerName}</span>
      </div>
    </div>
`;

  // Add form responses if any
  if (responses && responses.length > 0) {
    html += `    <div class="form-responses">
      <h2>Form Responses</h2>
`;
    for (const r of responses) {
      html += `      <div class="form-field">
        <div class="form-label">${escapeHtml(r.question_label)}</div>
        <div class="form-value">${escapeHtml(r.response || '(No response)')}</div>
      </div>
`;
    }
    html += `    </div>
`;
  }

  html += `    <div class="messages">
`;

  // Add messages
  for (const msg of messages) {
    const author = msg.author;
    const authorName = escapeHtml(author?.username || 'Unknown');
    const isBot = author?.bot || false;
    const avatarUrl = author?.displayAvatarURL({ format: 'png', size: 64 }) || getDefaultAvatar(author?.id || '0');
    const timestamp = formatTimestamp(msg.createdTimestamp);
    const content = markdownToHtml(msg.content);

    html += `      <div class="message">
        <img class="message-avatar" src="${avatarUrl}" alt="">
        <div class="message-content">
          <div class="message-header">
            <span class="message-author${isBot ? ' bot' : ''}">${authorName}</span>
            ${isBot ? '<span class="bot-tag">BOT</span>' : ''}
            <span class="message-time">${timestamp}</span>
          </div>
`;

    if (content) {
      html += `          <div class="message-text">${content}</div>
`;
    }

    // Add attachments
    if (msg.attachments.size > 0) {
      for (const [, attachment] of msg.attachments) {
        const isImage = attachment.contentType?.startsWith('image/') || /\.(png|jpg|jpeg|gif|webp)$/i.test(attachment.name);
        if (isImage) {
          html += `          <div class="attachment">
            <a href="${escapeHtml(attachment.url)}" target="_blank">
              <img src="${escapeHtml(attachment.url)}" alt="${escapeHtml(attachment.name)}">
            </a>
          </div>
`;
        } else {
          html += `          <div class="attachment">
            <a class="attachment-file" href="${escapeHtml(attachment.url)}" target="_blank">
              📎 ${escapeHtml(attachment.name)}
            </a>
          </div>
`;
        }
      }
    }

    // Add embeds
    if (msg.embeds && msg.embeds.length > 0) {
      for (const embed of msg.embeds) {
        const embedColor = embed.color ? `#${embed.color.toString(16).padStart(6, '0')}` : '#5865f2';
        html += `          <div class="embed" style="border-left-color: ${embedColor}">
`;
        if (embed.title) {
          html += `            <div class="embed-title">${escapeHtml(embed.title)}</div>
`;
        }
        if (embed.description) {
          html += `            <div class="embed-description">${markdownToHtml(embed.description)}</div>
`;
        }
        if (embed.fields && embed.fields.length > 0) {
          for (const field of embed.fields) {
            html += `            <div class="embed-field">
              <div class="embed-field-name">${escapeHtml(field.name)}</div>
              <div class="embed-field-value">${markdownToHtml(field.value)}</div>
            </div>
`;
          }
        }
        html += `          </div>
`;
      }
    }

    html += `        </div>
      </div>
`;
  }

  html += `    </div>
    <div class="footer">
      <p>Generated by METiers Bot</p>
      <p>${messages.length} messages in this transcript</p>
    </div>
  </div>
</body>
</html>`;

  return html;
}

module.exports = { buildTranscript, buildHtmlTranscript, fetchAllMessages };
