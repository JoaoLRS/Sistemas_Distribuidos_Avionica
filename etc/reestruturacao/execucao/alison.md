# Tarefas de Execução — Alison

Você é responsável pelo desenvolvimento e integração de **dois módulos distribuídos** com foco em Computação Crítica Altamente Disponível (Eleição de Líder) e Replicação Meteorológica P2P.

---

## Módulo 1: Computador de Voo Primário (Líder)

### Objetivo
Realizar processamentos de voo (como cálculo de velocidade vertical e posicionamento de piloto automático) com alta disponibilidade por meio de replicação ativa-passiva.

### Especificação Técnica
- **Tecnologia:** Python.
- **Lógica (Eleição de Líder - Bully Algorithm):**
  1. O Computador Primário inicia como **Líder** ativo e assume a responsabilidade de publicar dados de guiamento.
  2. Enviar pings/heartbeats de sinal de vida a cada 3 segundos em `avionica.module.health` e em um canal dedicado de keep-alive.
  3. Trocar mensagens com a instância do Computador Secundário (módulo do João Lucas Ribeiro).
  4. Se o Primário for interrompido ou falhar, o Secundário detectará a queda de ping e iniciará o **Bully Algorithm** para se auto-eleger como novo Líder.
  5. Se o Primário reatar a conexão depois de uma falha, ele deve desafiar o Secundário enviando uma mensagem `ELECTION` de prioridade mais alta para retomar a liderança de forma autônoma.
- **Produção:**
  - Publicar dados consolidados em `avionica.telemetry.navigation`.

---

## Módulo 2: Radar Climático B (Gossip Protocol)

### Objetivo
Simular e espalhar as informações climáticas da segunda zona de voo de forma descentralizada.

### Especificação Técnica
- **Tecnologia:** Python.
- **Lógica (Gossip Protocol):**
  1. O Radar B gera medições climáticas correspondentes à Região B.
  2. Conectar-se ao Radar A (módulo do Rafael) via sockets TCP ou HTTP.
  3. A cada 10 segundos, enviar os dados climáticos locais e receber os dados climáticos da Região A (processamento descentralizado e eventual).
  4. Mesclar as duas malhas climáticas de forma incremental na memória local.
- **Produção:**
  - Publicar a telemetria consolidada em `avionica.telemetry.radar`.
