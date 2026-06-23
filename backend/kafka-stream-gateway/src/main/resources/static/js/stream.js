/* stream.js — CDU-02: Monitor de Mensagens em Tempo Real */

const topicName = new URLSearchParams(window.location.search).get('name');
let paused = false;
let messageCount = 0;
let eventSource = null;


document.getElementById('topic-title').textContent = topicName || '(sem tópico)';
document.title = `${topicName} — Kafka Monitor`;

if (topicName) {
    connect();
} else {
    document.getElementById('empty-state').innerHTML =
        '<p>⚠️ Nenhum tópico especificado.</p><p class="hint"><a href="/" style="color:var(--accent)">Voltar para a lista</a></p>';
}

function connect() {
    eventSource = new EventSource(`/api/topics/${encodeURIComponent(topicName)}/stream`);

    eventSource.addEventListener('message', function (event) {

        const emptyState = document.getElementById('empty-state');
        if (emptyState) emptyState.remove();

        if (paused) return;

        const msg = JSON.parse(event.data);
        renderMessage(msg);
    });

    eventSource.addEventListener('open', function () {
        updateConnectionStatus(true);
    });

    eventSource.addEventListener('error', function () {
        updateConnectionStatus(false);
    });
}

function renderMessage(msg) {
    const container = document.getElementById('message-console');

    const row = document.createElement('div');
    row.className = 'message-row';

    let formattedPayload;
    try {
        formattedPayload = JSON.stringify(JSON.parse(msg.payload), null, 2);
    } catch {
        formattedPayload = msg.payload || '';
    }

    const ts = msg.timestamp;
    const displayTs = ts.includes('T') ? ts.replace('T', ' ').substring(0, 23) : ts;

    row.innerHTML =
        `<span class="msg-timestamp">${escapeHtml(displayTs)}</span>` +
        `<span class="msg-key">${escapeHtml(msg.key || '—')}</span>` +
        `<span class="msg-partition">P${msg.partition}:${msg.offset}</span>` +
        `<pre class="msg-payload">${escapeHtml(formattedPayload)}</pre>`;

    container.prepend(row);

    while (container.children.length > 500) {
        container.removeChild(container.lastChild);
    }

    messageCount++;
    document.getElementById('msg-counter').textContent = `${messageCount} mensagens recebidas`;
}

function togglePause() {
    paused = !paused;
    const btn = document.getElementById('pauseBtn');
    if (paused) {
        btn.textContent = '▶ Retomar';
        btn.classList.add('paused');
    } else {
        btn.textContent = '⏸ Pausar';
        btn.classList.remove('paused');
    }
}

function clearConsole() {
    document.getElementById('message-console').innerHTML = '';
    messageCount = 0;
    document.getElementById('msg-counter').textContent = '0 mensagens recebidas';
}

function updateConnectionStatus(connected) {
    const badge = document.getElementById('connection-status');
    if (connected) {
        badge.className = 'connection-badge connected';
        badge.innerHTML = '<span class="conn-dot"></span> Conectado';
    } else {
        badge.className = 'connection-badge';
        badge.innerHTML = '<span class="conn-dot"></span> Desconectado';
    }
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}