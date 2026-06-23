/* topics.js — CDU-01: Visão Geral dos Tópicos */

async function loadTopics() {
    try {
        const response = await fetch('/api/topics');
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const topics = await response.json();
        renderTopics(topics);
        updateStats(topics);
    } catch (error) {
        document.getElementById('topic-grid').innerHTML =
            '<div class="loading-state"><p>⚠️ Não foi possível conectar ao Kafka.</p><p class="hint">Verifique se o broker está ativo.</p></div>';
    }
}

function renderTopics(topics) {
    const grid = document.getElementById('topic-grid');

    if (topics.length === 0) {
        grid.innerHTML = '<div class="loading-state"><p>Nenhum tópico encontrado.</p><p class="hint">Os tópicos aparecerão aqui quando forem criados no broker.</p></div>';
        return;
    }

    grid.innerHTML = topics.map(t => `
        <a href="/topico.html?name=${encodeURIComponent(t.name)}" class="topic-card">
            <div class="topic-name">${escapeHtml(t.name)}</div>
            <div class="topic-stats">
                <span class="message-count">${t.messageCount.toLocaleString('pt-BR')}</span>
                <span class="message-label">mensagens</span>
            </div>
            <div class="topic-meta">
                <div class="topic-status ${t.status === 'Ativo' ? 'active' : ''}">
                    <span class="status-dot"></span>
                    ${t.status}
                </div>
                <span class="partition-count">${t.partitions} partição(ões)</span>
            </div>
            <div class="consumer-groups">
                ${t.consumerGroups.map(g => `<span class="group-tag">${escapeHtml(g)}</span>`).join('')}
            </div>
        </a>
    `).join('');
}

function updateStats(topics) {
    document.getElementById('total-topics').textContent = topics.length;
    const totalMsgs = topics.reduce((sum, t) => sum + t.messageCount, 0);
    document.getElementById('total-messages').textContent = totalMsgs.toLocaleString('pt-BR');
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}


loadTopics();
setInterval(loadTopics, 5000);