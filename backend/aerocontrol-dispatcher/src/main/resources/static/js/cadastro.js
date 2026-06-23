/* cadastro.js — CDU-01: Formulário de Cadastro de Aeronaves */

document.getElementById('aircraft-form').addEventListener('submit', async function (e) {
    e.preventDefault();

    const submitBtn = document.getElementById('submitBtn');
    submitBtn.disabled = true;
    submitBtn.textContent = '⏳ Salvando...';

    const data = {
        callsign: document.getElementById('callsign').value.trim(),
        modelo: document.getElementById('modelo').value.trim(),
        capacidade_combustivel: parseInt(document.getElementById('capacidade').value),
        velocidade_cruzeiro: parseInt(document.getElementById('velocidade').value)
    };

    try {
        const response = await fetch('/api/aircraft', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });

        const result = await response.json();

        if (response.ok) {
            showToast('Aeronave ' + (result.callsign || data.callsign) + ' criada com sucesso!', 'success');
            setTimeout(function () {
                window.location.href = '/aeronaves';
            }, 1000);
        } else {
            showToast(result.error || 'Erro ao criar aeronave.', 'error');
            submitBtn.disabled = false;
            submitBtn.textContent = '💾 Salvar Aeronave';
        }
    } catch (error) {
        showToast('Erro de conexão com o servidor.', 'error');
        submitBtn.disabled = false;
        submitBtn.textContent = '💾 Salvar Aeronave';
    }
});

function showToast(message, type) {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = 'toast ' + type;
    toast.textContent = message;
    container.appendChild(toast);
    setTimeout(function () { toast.remove(); }, 4000);
}