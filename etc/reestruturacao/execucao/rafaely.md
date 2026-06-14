# Tarefas de Execução — Rafaely

Você é responsável pelo desenvolvimento e integração de **dois módulos distribuídos** nas camadas de Apresentação Gráfica e Replicação de Logs Históricos.

---

## Módulo 1: SGCA Cockpit Simulator (Frontend)

### Objetivo
Liderar o desenvolvimento e aprimoramento da interface gráfica de simulação no frontend.

### Especificação Técnica
- **Tecnologia:** Javascript / Vue.js 3 (edita e implementa em `frontend/src`).
- **Lógica:**
  1. Desenvolver a tela principal do cockpit (Dashboard/PFD) utilizando o tema profissional Glass Cockpit.
  2. Implementar os widgets de instrumentos (fitas dinâmicas de altitude e velocidade, bússola/proa, horizonte artificial para Pitch e Roll).
  3. Consumir a conexão de **Server-Sent Events (SSE)** em `http://localhost:8080/api/telemetry/stream` (ou polling se SSE for simplificado) para atualizar os medidores em tempo real sem interrupção.
  4. Tratar o estado de carregamento de rotas pendentes (exibindo avisos amigáveis de cálculo).

---

## Módulo 2: Caixa Preta / FDR Replicada (Master-Slave)

### Objetivo
Gravar de forma altamente disponível todos os eventos críticos do voo por meio de replicação ativa para evitar perda de dados históricos.

### Especificação Técnica
- **Tecnologia:** Python.
- **Lógica (Replicação Ativo-Passivo com Quorum):**
  1. O processo consome todas as mensagens de eventos do tópico Kafka `avionica.system.events` e telemetria consolidada `avionica.telemetry.motor.consolidated`.
  2. Funcionar no modelo **Master-Slave**: possui um nó gravador principal (Master) e um nó réplica (Slave) em containers separados.
  3. Ao receber uma mensagem, o Master grava o log em seu disco local e envia uma cópia síncrona via rede para o Slave.
  4. O log de voo só é considerado gravado com sucesso após o Slave responder com confirmação de escrita (Quorum de confirmação).
  5. Se o Slave estiver indisponível, o Master deve continuar a gravação de forma local (estado degradado) e alertar no barramento.
