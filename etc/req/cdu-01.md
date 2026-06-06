# CDU-01: Monitoramento de Telemetria em Tempo Real

## 1. Descrição
Permite que o usuário visualize os dados gerados pelos diversos sensores da aeronave (Voo, Freios, Radar, WAIC, Navegação) em tempo real, fornecendo um panorama imediato do status do sistema.

## 2. Atores
- **Usuário:** Piloto, engenheiro ou operador que está acessando a interface web.
- **Sistema Produtor (Sensores):** Módulos Python gerando dados de forma contínua via Kafka.

## 3. Pré-condições
- A aplicação web e o backend devem estar ativos e comunicando-se.
- Os simuladores/sensores devem estar publicando mensagens no barramento Kafka.

## 4. Fluxo Principal
1. O usuário acessa a URL da aplicação web (`http://localhost:5173`).
2. O sistema exibe diretamente a **Tela de Dashboard (Telemetria Principal)**.
3. A tela apresenta componentes visuais agrupados por módulo:
   - **Indicadores de Voo:** Combustível (%), Altitude (ft), Velocidade (Mach).
   - **Indicadores de Freio:** Pressão Hidráulica (PSI).
   - **Radar:** Condição Climática, Velocidade do Vento.
   - **WAIC:** Pressão e Temperatura dos motores.
4. O Frontend requisita continuamente os dados mais recentes da API REST do Backend (via *Polling* periódico ou *WebSockets/SSE*).
5. O Backend lê a última telemetria salva no PostgreSQL (ou a intercepta diretamente na memória).
6. O Frontend recebe a resposta e atualiza os gráficos e painéis numéricos na tela sem a necessidade de recarregar a página.

## 5. Fluxos Alternativos
- **5a. Perda de Conexão com os Sensores ou Backend:**
  1. O Frontend para de receber respostas válidas do Backend, ou o Backend nota que a última atualização dos sensores é muito antiga.
  2. O sistema altera o status visual do componente de telemetria afetado para **"Sem Sinal"** ou **"Offline"**.
  3. Um alerta amigável é exibido no topo da tela: *"Sinal de telemetria perdido. Tentando reconectar..."*.

## 6. Pós-condições
- O usuário possui uma visão clara e atualizada de todos os sensores da aeronave para rápida tomada de decisão.
