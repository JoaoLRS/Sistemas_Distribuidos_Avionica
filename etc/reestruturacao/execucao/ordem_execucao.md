# Ordem de Execução das Tarefas

Para garantir que o sistema distribuído seja desenvolvido sem bloqueios entre os integrantes, a equipe deve seguir a seguinte ordem de execução dividida em 4 fases sequenciais:

---

## Fase 1: Fundação do Backend e Banco de Dados (Prioridade Máxima)
*Esta fase garante que as APIs REST de persistência e os canais de mensagens estejam operacionais para os demais módulos consumirem.*

1.  **Mariana** e **Nickolas** realizam o build inicial do backend-gateway.
2.  Garantir que as tabelas de `aeronaves` e `rotas_fms` sejam inicializadas no PostgreSQL via auto-DDL do Spring Boot (`schema.sql`).
3.  Testar localmente a inserção de aeronaves e gravação da telemetria de testes no banco.

---

## Fase 2: Portais Web e Cockpit
*Esta fase fornece as interfaces gráficas e portas locais para que possamos controlar o ciclo de vida do simulador.*

4.  **Ana Luisa** cria os formulários da **Torre de Comando (:8082)** e o **Painel do Kafka (:9000)**.
5.  **Rafaely** desenvolve a tela do **SGCA Cockpit (:5173)** e os widgets analógicos.
6.  **João Lucas Ribeiro** desenvolve a interface do **Visualizador de Banco de Dados (:8081)**.
7.  Integrar todas as telas às APIs REST expostas pelo Backend Gateway.

---

## Fase 3: Simuladores e Sensores Base
*Aqui os sensores passam a rodar em containers gerando tráfego no barramento.*

8.  **João Lucas Cosme** desenvolve e sobe as instâncias dos Motores Redundantes A, B e C, conectando ao Kafka.
9.  **Rafael** finaliza o FMS com o fallback Dijkstra offline ativo caso a API do Ninja falhe.
10. Sobe-se os sensores base de altitude, velocidade e freios, testando a movimentação básica no painel de telemetria do SGCA.

---

## Fase 4: Algoritmos Distribuídos Avançados (Consenso, Relógios e Locks)
*Fase em que o sistema distribuído ganha inteligência avançada para apresentação.*

11. **Gabriela** ativa o módulo **Voter TMR** consumindo os motores A, B e C para filtrar falhas.
12. **Alison** e **João Lucas Ribeiro** implementam os Computadores de Voo Primário e Secundário trocando heartbeats e ativam a eleição por **Bully Algorithm** se um deles for derrubado.
13. **Mariana** e **Nickolas** integram o **tempo de Cristian** nos pings de telemetria e a gravação de logs ordenados por **Lamport Timestamps**.
14. Executar testes de injeção de falhas (**João Lucas Cosme**) para comprovar a tolerância a falhas na apresentação.
