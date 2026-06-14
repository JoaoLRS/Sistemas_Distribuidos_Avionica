# Tarefas de Execução — Gabriela

Você é responsável pelo desenvolvimento e integração de **dois módulos distribuídos** com foco em Consenso e Tolerância a Falhas.

---

## Módulo 1: Voter Consensual do Motor (TMR)

### Objetivo
Evitar que a leitura errada de um único sensor de motor (Motor A, B ou C) envie dados corrompidos para a cabine de pilotagem.

### Especificação Técnica
- **Tecnologia:** Python (em execução em um container separado).
- **Consumo:** Inscrever-se nos tópicos Kafka `avionica.telemetry.motor.a`, `avionica.telemetry.motor.b` e `avionica.telemetry.motor.c`.
- **Lógica (Votação por Redundância Modular Tripla):**
  1. Aguardar a chegada de payloads dos 3 motores.
  2. Comparar as temperaturas e pressões lidas.
  3. Se todos os três valores forem semelhantes (variação < 5%), calcular a média aritmética simples e assumi-la.
  4. Se um motor estiver divergente (variação > 5% dos outros dois), **descarte a leitura dele** (identificando-o como nó falho), calcule a média dos outros dois e publique o resultado consolidado.
  5. Se todos os três divergirem (falha bizantina insolúvel), envie um alerta crítico.
- **Produção:**
  - Publicar a telemetria consolidada em `avionica.telemetry.motor.consolidated`.
  - Se um motor falhar, enviar um evento em `avionica.system.events` com severidade `WARNING` (Ex: `"Sensor do Motor B falhou - valor discrepante descartado"`).

---

## Módulo 2: Detector de Falhas (Heartbeats)

### Objetivo
Monitorar em tempo real a disponibilidade de todos os containers ativos do sistema e alertar em caso de queda de qualquer processo.

### Especificação Técnica
- **Tecnologia:** Python ou Java.
- **Consumo:** Escutar o tópico Kafka `avionica.module.health`.
- **Lógica (Phi Accrual / Heartbeat Timeout):**
  1. Todos os módulos (sensores, computadores e FMS) enviarão uma mensagem a cada 5 segundos para o tópico de health contendo `{"callsign": "...", "modulo": "nome-do-modulo", "timestamp": ...}`.
  2. O Detector de Falhas deve manter uma tabela em memória com a data e hora do último ping recebido de cada módulo.
  3. Se um módulo ficar mais de 15 segundos sem enviar batimentos cardíacos, mude seu status para `DOWN` no banco de dados (`module_status`) e gere um alerta.
- **Produção:**
  - Publicar alertas de indisponibilidade em `avionica.alerts.generated` (Ex: `{"tipo": "FALHA_CONEXAO", "descricao": "Computador de Voo Primario offline", "severidade": "CRITICAL"}`).
