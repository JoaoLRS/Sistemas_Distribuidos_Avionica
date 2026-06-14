# Tarefas de Execução — Ana Luisa

Você é responsável pelo desenvolvimento e integração de **dois módulos distribuídos** voltados para gerenciamento de infraestrutura e do ciclo de vida das aeronaves via Web.

---

## Módulo 1: Painel do Kafka (Localhost)

### Objetivo
Fornecer um painel web que mostre de forma transparente o tráfego de dados e eventos que passam pelos tópicos Kafka do simulador.

### Especificação Técnica
- **Tecnologia:** Vue.js ou Node.js (rodando na porta `:9000`).
- **Lógica:**
  1. Conectar-se ao Broker do Kafka (`localhost:9092`).
  2. Listar os tópicos ativos do sistema (ex: `avionica.telemetry.motor.consolidated`, `avionica.route.calculated`).
  3. Ao selecionar um tópico, abrir um terminal web em tempo real (usando Server-Sent Events ou WebSockets encapsulados) que exibe o timestamp e o payload de cada mensagem que transita por aquele canal.
  4. Adicionar botão de Pausar e Limpar o log visual para facilitar a demonstração.

---

## Módulo 2: Torre de Comando (Localhost)

### Objetivo
Liderar o desenvolvimento do portal de cadastro de aeronaves, servindo como o ponto de partida de todas as simulações.

### Especificação Técnica
- **Tecnologia:** Vue.js ou Node.js (rodando na porta `:8082`).
- **Lógica:**
  1. Criar um formulário web com campos: prefixo/callsign (ex: PR-AAA), modelo, capacidade de combustível e velocidade de cruzeiro.
  2. Efetuar um `POST /api/aircraft` para o Backend Gateway gravando os dados no PostgreSQL (status inicial `No Patio`).
  3. Exibir a lista de todas as aeronaves cadastradas e seus respectivos status de simulação em tempo real (`No Patio`, `Em Preparacao`, `Em Voo`).
  4. Adicionar um botão de exclusão que chama `DELETE /api/aircraft/{callsign}` (bloqueando a exclusão caso a aeronave esteja `Em Voo`).
