# Tarefas de Execução — Nickolas

Você é responsável pelo desenvolvimento e integração de **dois módulos distribuídos** com foco em Ordenamento Lógico de Eventos e Exclusão Mútua Distribuída.

---

## Módulo 1: Persistência Ordenada (Lamport Timestamps)

### Objetivo
Evitar que dados de telemetria ou logs cheguem fora de ordem cronológica e corrompam o histórico devido à latência variável da rede.

### Especificação Técnica
- **Tecnologia:** Java / Spring Boot ou Python.
- **Lógica (Relógio Lógico de Lamport):**
  1. Todos os sensores do sistema enviarão em suas mensagens um número inteiro representando seu Relógio Lógico de Lamport (`logical_clock`).
  2. O consumidor do banco lê a telemetria recebida do Kafka.
  3. Antes de inserir no PostgreSQL (`mensagens_barramento`), o consumidor atualiza seu próprio relógio de Lamport: $Clock_{local} = \max(Clock_{local}, Clock_{mensagem}) + 1$.
  4. Manter uma janela deslizante (buffer temporário de ordenação de mensagens na memória).
  5. Gravar os dados no PostgreSQL ordenados estritamente pelo Relógio Lógico de Lamport consolidado, garantindo a causalidade lógica das ações.

---

## Módulo 2: Sensor de Freios com Lock Distribuído (Ricart-Agrawala)

### Objetivo
Garantir que múltiplos sistemas autônomos ou manuais (Piloto Automático, Comando Manual e Atuador Automático de Pouso) solicitem permissão exclusiva e coordenada para acionar o sistema hidráulico de freios da aeronave.

### Especificação Técnica
- **Tecnologia:** Python.
- **Lógica (Ricart-Agrawala):**
  1. O sensor de freios simula o acionamento físico do sistema hidráulico.
  2. Para acionar os freios, o módulo deve solicitar permissão (um lock distribuído) a pelo menos outros 2 computadores de voo ativos no ecossistema ( Alison e João Lucas Ribeiro).
  3. Enviar mensagem de `REQUEST` contendo o ID do módulo e o timestamp de Lamport.
  4. Responder aos requests de outros nós com base na ordem dos timestamps (o menor timestamp ganha prioridade).
  5. Acionar o atuador de freio apenas após receber o `OK / GRANT` de todos os nós participantes.
- **Produção:**
  - Publicar a telemetria de freios no tópico `avionica.telemetry.brakes`.
