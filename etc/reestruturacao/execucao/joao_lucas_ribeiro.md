# Tarefas de Execução — João Lucas Ribeiro

Você é responsável pelo desenvolvimento e integração de **dois módulos distribuídos** com foco em Computação de Backup Resiliente (Eleição de Líder) e Auditoria Gráfica do Banco de Dados.

---

## Módulo 1: Computador de Voo Secundário (Backup)

### Objetivo
Assumir as operações críticas de posicionamento e guia de voo de forma autônoma caso o computador primário caia no meio do trajeto.

### Especificação Técnica
- **Tecnologia:** Python.
- **Lógica (Bully Algorithm):**
  1. Iniciar como nó de **Backup (Passivo)**, apenas escutando o canal de pings do Computador Primário ( Alison).
  2. Monitorar os heartbeats. Se o líder ficar mais de 6 segundos sem enviar sinal de vida (timeout), assumir que o Primário caiu.
  3. Iniciar o **Bully Algorithm** enviando mensagens de eleição. Como o Secundário é o nó de maior prioridade ativo após a queda do primário, declarar-se líder publicando um aviso no Kafka.
  4. Passar a publicar ativamente os cálculos de guiamento em `avionica.telemetry.navigation`.
  5. Se receber uma mensagem de `ELECTION` de maior prioridade (quando o Primário ressurgir), retornar imediatamente ao estado passivo de backup.

---

## Módulo 2: Visualizador de Banco de Dados (Localhost)

### Objetivo
Criar uma interface gráfica web simples e isolada para permitir que o usuário verifique dados históricos no PostgreSQL sem instalar ferramentas externas.

### Especificação Técnica
- **Tecnologia:** Vue.js ou Node.js (rodando na porta `:8081`).
- **Lógica:**
  1. Conectar-se ao banco de dados PostgreSQL do projeto.
  2. Exibir a lista de tabelas aviônicas persistidas (`telemetry_voo`, `rotas_fms`, `alertas`, `mensagens_barramento`).
  3. Criar uma tela de busca por texto no payload JSON das mensagens e filtros temporais básicos (últimas 50, 100 ou 500 mensagens gravadas).
  4. Adicionar um botão administrativo para limpar tabelas de testes (executando comandos SQL `TRUNCATE`).
