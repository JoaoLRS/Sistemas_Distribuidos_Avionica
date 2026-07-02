/* aeronaves.js — CDU-02: Lista de Aeronaves Ativas */

async function loadAircraft() {
    try {
        const response = await fetch('/api/aircraft');
        if (!response.ok) throw new Error('HTTP ' + response.status);
        const aircraft = await response.json();
        renderAircraft(aircraft);
    } catch (error) {
        document.getElementById('aircraft-body').innerHTML =
            '<tr><td colspan="7" class="loading-cell">⚠️ Erro ao carregar aeronaves.</td></tr>';
    }
}

function renderAircraft(aircraft) {
    const tbody = document.getElementById('aircraft-body');
    const emptyMsg = document.getElementById('empty-message');
    const countBadge = document.getElementById('aircraft-count');

    countBadge.textContent = aircraft.length + ' aeronave(s)';

    if (aircraft.length === 0) {
        tbody.parentElement.parentElement.style.display = 'none';
        emptyMsg.style.display = 'block';
        return;
    }

    tbody.parentElement.parentElement.style.display = '';
    emptyMsg.style.display = 'none';

    tbody.innerHTML = aircraft.map(function (a) {
        var statusClass = getStatusClass(a.status);
        var isInFlight = a.status === 'Em Voo';

        return '<tr>' +
            '<td class="callsign-cell">' + escapeHtml(a.callsign) + '</td>' +
            '<td>' + escapeHtml(a.modelo || '—') + '</td>' +
            '<td>' + (a.capacidade_combustivel != null ? Number(a.capacidade_combustivel).toLocaleString('pt-BR') : '—') + '</td>' +
            '<td>' + (a.velocidade_cruzeiro != null ? a.velocidade_cruzeiro : '—') + '</td>' +
            '<td><span class="status-badge ' + statusClass + '">' + escapeHtml(a.status) + '</span></td>' +
            '<td>' + formatDate(a.ultima_atualizacao) + '</td>' +
            '<td>' +
            '<button class="btn btn-danger" onclick="deleteAircraft(\'' + escapeHtml(a.callsign) + '\')"' +
            (isInFlight ? ' disabled title="Não é possível excluir aeronave em voo"' : '') +
            '>🗑 Excluir</button>' +
            (isInFlight ? ' <button class="btn btn-warning" onclick="abortFlight(\'' + escapeHtml(a.callsign) + '\')">⚠️ Abortar Voo</button>' : '') +
            '</td>' +
            '</tr>';
    }).join('');
}

async function abortFlight(callsign) {
    if (!confirm('Deseja abortar o voo da aeronave ' + callsign + ' e liberá-la para o pátio?')) return;

    try {
        var response = await fetch('/api/aircraft/' + encodeURIComponent(callsign) + '/status', {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ status: 'Em Preparacao' })
        });
        var result = await response.json();

        if (response.ok) {
            showToast('Voo da aeronave ' + callsign + ' abortado. Status atualizado para Em Preparacao.', 'success');
            loadAircraft();
        } else {
            showToast(result.error || 'Erro ao abortar voo.', 'error');
        }
    } catch (error) {
        showToast('Erro de conexão.', 'error');
    }
}

async function deleteAircraft(callsign) {
    if (!confirm('Confirma a exclusão da aeronave ' + callsign + '?')) return;

    try {
        var response = await fetch('/api/aircraft/' + encodeURIComponent(callsign), {
            method: 'DELETE'
        });
        var result = await response.json();

        if (response.ok) {
            showToast('Aeronave ' + callsign + ' removida.', 'success');
            loadAircraft();
        } else {
            showToast(result.error || 'Erro ao excluir.', 'error');
        }
    } catch (error) {
        showToast('Erro de conexão.', 'error');
    }
}

function getStatusClass(status) {
    if (status === 'No Patio') return 'status-no-patio';
    if (status === 'Em Preparacao') return 'status-em-preparacao';
    if (status === 'Em Voo') return 'status-em-voo';
    return '';
}

function formatDate(dateStr) {
    if (!dateStr) return '—';
    try {
        var d = new Date(dateStr);
        return d.toLocaleString('pt-BR', {
            day: '2-digit', month: '2-digit', year: 'numeric',
            hour: '2-digit', minute: '2-digit', second: '2-digit'
        });
    } catch (e) {
        return dateStr;
    }
}

function escapeHtml(text) {
    if (!text) return '';
    var div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function showToast(message, type) {
    var container = document.getElementById('toast-container');
    var toast = document.createElement('div');
    toast.className = 'toast ' + type;
    toast.textContent = message;
    container.appendChild(toast);
    setTimeout(function () { toast.remove(); }, 4000);
}

loadAircraft();
setInterval(loadAircraft, 3000);