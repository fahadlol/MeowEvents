const guildSelect = document.getElementById('guildSelect');
const panelsSection = document.getElementById('panelsSection');
const panelsList = document.getElementById('panelsList');
const formSection = document.getElementById('formSection');
const formTitle = document.getElementById('formTitle');
const panelForm = document.getElementById('panelForm');
const btnNewPanel = document.getElementById('btnNewPanel');
const btnCancelForm = document.getElementById('btnCancelForm');
const emptyState = document.getElementById('emptyState');
const botOffline = document.getElementById('botOffline');

const panelChannel = document.getElementById('panelChannel');
const panelRole = document.getElementById('panelRole');
const panelCategory = document.getElementById('panelCategory');
const panelLogChannel = document.getElementById('panelLogChannel');
const panelButtonEmoji = document.getElementById('panelButtonEmoji');
const panelAutoCloseDays = document.getElementById('panelAutoCloseDays');
const panelEmbedColor = document.getElementById('panelEmbedColor');
const panelEditId = document.getElementById('panelEditId');
const panelSubmitBtn = document.getElementById('panelSubmitBtn');

async function api(path, opts = {}) {
  const res = await fetch(path, { credentials: 'same-origin', ...opts });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.error || res.statusText || 'Request failed');
  return data;
}

async function loadGuilds() {
  try {
    const guilds = await api('/api/guilds');
    guildSelect.innerHTML = '<option value="">— Select server —</option>' +
      guilds.map((g) => `<option value="${g.id}">${escapeHtml(g.name)}</option>`).join('');
    botOffline.hidden = true;
  } catch (e) {
    guildSelect.innerHTML = '<option value="">— Select server —</option>';
    botOffline.hidden = false;
    botOffline.textContent = 'Bot is offline or unreachable. Start the bot and ensure API_SECRET matches.';
  }
}

function escapeHtml(s) {
  const div = document.createElement('div');
  div.textContent = s;
  return div.innerHTML;
}

guildSelect.addEventListener('change', async () => {
  const guildId = guildSelect.value;
  panelsSection.hidden = !guildId;
  emptyState.hidden = true;
  formSection.hidden = true;
  if (!guildId) return;

  try {
    const list = await api(`/api/panels?guildId=${guildId}`);
    renderPanels(list);
    if (list.length === 0) emptyState.hidden = false;
  } catch (e) {
    panelsList.innerHTML = '<p class="hint">Could not load panels.</p>';
  }
});

function renderPanels(list) {
  if (list.length === 0) {
    panelsList.innerHTML = '';
    emptyState.hidden = false;
    return;
  }
  emptyState.hidden = true;
  panelsList.innerHTML = list
    .map(
      (p) =>
        `<div class="panel-item" data-id="${p.id}">
          <div>
            <span class="title">${escapeHtml(p.title)}</span>
            <div class="meta">Channel: ${escapeHtml(p.channel_id)} · Role: ${escapeHtml(p.role_id)} · <strong>${p.openCount ?? 0} open</strong></div>
          </div>
          <div class="panel-actions">
            <button type="button" class="btn btn-ghost edit-panel" data-id="${p.id}">Edit</button>
            <button type="button" class="btn btn-danger delete-panel" data-id="${p.id}">Delete</button>
          </div>
        </div>`
    )
    .join('');

  panelsList.querySelectorAll('.delete-panel').forEach((btn) => {
    btn.addEventListener('click', async () => {
      if (!confirm('Delete this panel? The Discord message will be removed.')) return;
      try {
        await api(`/api/panels/${btn.dataset.id}`, { method: 'DELETE' });
        const guildId = guildSelect.value;
        const list = await api(`/api/panels?guildId=${guildId}`);
        renderPanels(list);
      } catch (e) {
        alert(e.message);
      }
    });
  });

  panelsList.querySelectorAll('.edit-panel').forEach((btn) => {
    btn.addEventListener('click', async () => {
      try {
        const panel = await api(`/api/panels/${btn.dataset.id}`);
        panelEditId.value = panel.id;
        formTitle.textContent = 'Edit ticket panel';
        panelSubmitBtn.textContent = 'Save changes';
        document.getElementById('panelTitle').value = panel.title || '';
        document.getElementById('panelDescription').value = panel.description || '';
        document.getElementById('panelButtonLabel').value = panel.button_label || '';
        panelButtonEmoji.value = panel.button_emoji || '';
        panelLogChannel.value = panel.log_channel_id || '';
        panelAutoCloseDays.value = panel.auto_close_days || 0;
        panelEmbedColor.value = (panel.embed_color || '').replace(/^#/, '');
        document.getElementById('panelCustomMessage').value = panel.custom_message || '';
        panelCategory.value = panel.category_id || '';
        panelRole.value = panel.role_id || '';
        panelChannel.value = panel.channel_id || '';
        panelChannel.disabled = true;
        await loadPanelFormOptions();
        panelChannel.value = panel.channel_id || '';
        panelChannel.disabled = true;
        formSection.hidden = false;
      } catch (e) {
        alert(e.message);
      }
    });
  });
}

btnNewPanel.addEventListener('click', () => {
  panelEditId.value = '';
  formTitle.textContent = 'New ticket panel';
  panelSubmitBtn.textContent = 'Create panel';
  panelForm.reset();
  panelChannel.disabled = false;
  loadPanelFormOptions();
  formSection.hidden = false;
});

btnCancelForm.addEventListener('click', () => {
  formSection.hidden = true;
});

async function loadPanelFormOptions() {
  const guildId = guildSelect.value;
  if (!guildId) return;
  try {
    const [channels, roles, categories] = await Promise.all([
      api(`/api/guilds/${guildId}/channels`),
      api(`/api/guilds/${guildId}/roles`),
      api(`/api/guilds/${guildId}/categories`),
    ]);
    panelChannel.innerHTML = channels.map((c) => `<option value="${c.id}"># ${escapeHtml(c.name)}</option>`).join('');
    panelRole.innerHTML = roles.map((r) => `<option value="${r.id}">@ ${escapeHtml(r.name)}</option>`).join('');
    panelCategory.innerHTML = '<option value="">None</option>' + categories.map((c) => `<option value="${c.id}">${escapeHtml(c.name)}</option>`).join('');
    panelLogChannel.innerHTML = '<option value="">None</option>' + channels.map((c) => `<option value="${c.id}"># ${escapeHtml(c.name)}</option>`).join('');
  } catch (e) {
    alert('Could not load channels/roles.');
  }
}

panelForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const guildId = guildSelect.value;
  if (!guildId) return;
  const editId = panelEditId.value ? parseInt(panelEditId.value, 10) : null;
  const autoClose = parseInt(panelAutoCloseDays.value, 10);
  const embedColor = panelEmbedColor.value.trim().replace(/^#/, '') || undefined;

  if (editId) {
    const payload = {
      roleId: panelRole.value,
      categoryId: panelCategory.value || undefined,
      title: document.getElementById('panelTitle').value.trim() || 'Support Tickets',
      description: document.getElementById('panelDescription').value.trim() || 'Click the button below to open a ticket.',
      buttonLabel: document.getElementById('panelButtonLabel').value.trim() || 'Open Ticket',
      buttonEmoji: panelButtonEmoji.value.trim() || undefined,
      logChannelId: panelLogChannel.value || undefined,
      autoCloseDays: Number.isNaN(autoClose) || autoClose <= 0 ? undefined : autoClose,
      embedColor: embedColor || undefined,
      customMessage: document.getElementById('panelCustomMessage').value.trim() || undefined,
    };
    try {
      await api(`/api/panels/${editId}`, { method: 'PATCH', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
      formSection.hidden = true;
      const list = await api(`/api/panels?guildId=${guildId}`);
      renderPanels(list);
    } catch (err) {
      alert(err.message);
    }
    return;
  }

  const payload = {
    guildId,
    channelId: panelChannel.value,
    roleId: panelRole.value,
    categoryId: panelCategory.value || undefined,
    title: document.getElementById('panelTitle').value.trim() || 'Support Tickets',
    description: document.getElementById('panelDescription').value.trim() || 'Click the button below to open a ticket.',
    buttonLabel: document.getElementById('panelButtonLabel').value.trim() || 'Open Ticket',
    buttonEmoji: panelButtonEmoji.value.trim() || undefined,
    logChannelId: panelLogChannel.value || undefined,
    autoCloseDays: Number.isNaN(autoClose) || autoClose <= 0 ? undefined : autoClose,
    embedColor: embedColor || undefined,
    customMessage: document.getElementById('panelCustomMessage').value.trim() || undefined,
  };
  try {
    await api('/api/panels', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
    formSection.hidden = true;
    const list = await api(`/api/panels?guildId=${guildId}`);
    renderPanels(list);
    emptyState.hidden = true;
  } catch (err) {
    alert(err.message);
  }
});

loadGuilds();
