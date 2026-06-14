# Tarefas de Execução — Mariana

Você é responsável pelo desenvolvimento e integração de **dois módulos distribuídos** na camada central (Java / Spring Boot), focando em Tolerância a Falhas de APIs externas e Sincronização de Relógios.

---

## Módulo 1: Backend Gateway Central (Circuit Breaker)

### Objetivo
Lidar com o desacoplamento de chamadas lentas para serviços de terceiros e APIs externas sem comprometer a estabilidade do gateway.

### Especificação Técnica
- **Tecnologia:** Java / Spring Boot (edita a base de código do `backend-gateway`).
- **Lógica (Circuit Breaker & Fallback):**
  1. Integrar um mecanismo de **Circuit Breaker** (usando a biblioteca Resilience4j ou lógica customizada no Spring Boot) na comunicação com serviços de rotas externas (FMS).
  2. Configurar limites de timeout (ex: se o FMS demorar mais de 3 segundos para responder ou calcular a rota, disparar o fallback).
  3. No Fallback, retornar um status degradado aceitável e manter o gateway rodando para receber outras requisições, evitando o efeito cascata de travamento em cascata de threads HTTP.

---

## Módulo 2: Servidor de Sincronização (Algoritmo de Cristian)

### Objetivo
Atuar como uma fonte centralizada de tempo (Time Server) para que todos os sensores distribuídos possam calibrar seus relógios locais contra o atraso de rede (RTT).

### Especificação Técnica
- **Tecnologia:** Java / Spring Boot.
- **Lógica (Cristian's Algorithm):**
  1. Criar um endpoint REST `GET /api/time-sync` no backend.
  2. Este endpoint deve retornar o tempo do servidor em nanossegundos: `{"server_time": ...}`.
  3. Quando os sensores Python enviarem dados, eles deverão:
     - Gravar o timestamp local imediato antes da requisição ($T_0$).
     - Chamar o endpoint `/api/time-sync`.
     - Gravar o timestamp local após o retorno ($T_1$).
     - Estimar o tempo de ida e volta da rede: $RTT = (T_1 - T_0)$.
     - Calibrar o relógio local do sensor: $TempoSync = ServerTime + (RTT / 2)$.
     - Utilizar o $TempoSync$ nas mensagens subsequentes publicadas no Kafka, demonstrando a sincronização distribuída.
