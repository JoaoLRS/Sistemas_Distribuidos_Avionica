# CDU-05: Monitorização de Consenso dos Motores (Algoritmo TMR)

## 1. Descrição
Permite ao utilizador acompanhar, em tempo real, o funcionamento do algoritmo de Tolerância a Falhas (Triple Modular Redundancy - TMR). O ecrã deve demonstrar como o sistema lê dados de três sensores redundantes de um mesmo motor e aplica o Consenso Bizantino para entregar um dado limpo e fiável, mesmo em caso de falhas periféricas.

## 2. Atores
- **Utilizador:** Engenheiro de voo ou piloto que visualiza o painel de redundância.
- **Módulo de Consenso (`consenso_motor.py`):** Microsserviço Python que assina os tópicos dos 3 sensores, calcula a maioria e publica o resultado final.

## 3. Pré-condições
- Os três nós sensores do motor (Canal A, Canal B e Canal C) devem estar a publicar dados no Kafka.
- O Frontend e o Backend devem estar a comunicar normalmente.

## 4. Fluxo Principal
1. O utilizador acede ao separador/menu **"Motores / Redundância TMR"** no Frontend.
2. O sistema exibe um painel contendo três medidores individuais (Sensor A, Sensor B, Sensor C) e um medidor principal destacado (Resultado Consolidado).
3. O Frontend consome do Backend os dados das três leituras brutas e a leitura final validada.
4. O utilizador observa que os três sensores publicam valores idênticos ou com variações mínimas aceitáveis (ex: 800°C, 801°C, 800°C).
5. O painel indica o estado do motor como **"Saudável - Consenso Estabelecido"**.

## 5. Fluxos Alternativos
- **5a. Falha num dos Sensores (Divergência):**
  1. O utilizador (ou o simulador de injeção de falhas) faz com que o Sensor B envie dados ruidosos ou congele (ex: 0°C ou 9999°C).
  2. A interface atualiza o medidor do Sensor B, que fica vermelho (estado "Anómalo").
  3. O algoritmo TMR no barramento descarta o dado do Sensor B e utiliza a média/consenso dos Sensores A e C.
  4. O painel principal (Resultado Consolidado) continua a exibir o valor correto (ex: 800°C), mantendo a integridade da aeronave.
  5. Um aviso de manutenção nível **WARNING** é gerado ("Falha de Sensor Isolada - Votação Maioritária Ativa").

## 6. Pós-condições
- A resiliência do sistema distribuído é garantida e exibida visualmente, provando que falhas simples em nós periféricos não deitam abaixo a leitura central.

---

# CDU-06: Monitorização de Ações Autónomas (Anti-gelo e TCAS)

## 1. Descrição
Permite ao utilizador visualizar o acionamento autónomo de sistemas críticos baseados em condições externas. O foco não é apenas visualizar a leitura, mas confirmar que a rede reagiu de forma automática (Machine-to-Machine) via barramento Kafka, atuando sem intervenção humana.

## 2. Atores
- **Utilizador:** Piloto a monitorizar o Glass Cockpit (Frontend Vue).
- **Nós Autónomos:** Simuladores (`sensor_clima.py`, `tcas.py`).

## 3. Pré-condições
- O sistema de mensagens está ativo e os módulos autónomos estão ligados ao barramento.

## 4. Fluxo Principal
1. O utilizador está com o ecrã do Dashboard Principal aberto.
2. O simulador de clima (`sensor_clima.py`) capta uma queda drástica de temperatura combinada com humidade (indicativo de formação de gelo).
3. O próprio nó distribuidor publica uma mensagem de comando de atuação no respetivo tópico.
4. O Backend lê essa atuação e guarda na base de dados PostgreSQL.
5. O Frontend recebe a atualização e altera o ícone/indicador do sistema Anti-gelo (Pitot/Asas) de cinzento (Desligado) para verde (Ativo/On).
6. Um pop-up não-intrusivo notifica o piloto: *"Sistema Anti-gelo acionado automaticamente"* (podendo incluir áudio sintético, se configurado).

## 5. Fluxos Alternativos
- **5a. Alerta de Colisão Iminente (TCAS):**
  1. O módulo TCAS identifica tráfego aéreo a cruzar a mesma altitude.
  2. A mensagem sobe ao barramento como evento de severidade máxima.
  3. A interface sobrepõe um alerta visual intermitente no ecrã e indica a manobra evasiva (ex: *PULL UP*).

## 6. Pós-condições
- Fica evidenciado o comportamento de *Publisher/Subscriber* reativo, onde os nós tomam decisões e o backend/frontend atuam apenas como exibidores de estado.

---

# CDU-07: Exportação de Auditoria da Caixa Preta

## 1. Descrição
Permite a extração dos registos brutos e consolidados da aeronave gerados pelo módulo Read-Only da "Caixa Preta". Essencial para análises pós-voo ou em caso de simulação de acidentes.

## 2. Atores
- **Utilizador:** Auditor de voo ou Engenheiro de Manutenção.
- **Módulo de Auditoria (`caixa_preta.py`):** Módulo passivo que grava tudo em ficheiros de baixo nível.

## 3. Pré-condições
- Existência de dados de telemetria já consolidados no PostgreSQL e persistidos pelos nós de auditoria.

## 4. Fluxo Principal
1. O utilizador acede ao ecrã **"Caixa Preta / Auditoria"** no Frontend.
2. O utilizador clica no botão **"Gerar Relatório de Voo (FDR)"**.
3. O Frontend dispara uma requisição de relatório para o Backend Spring Boot.
4. O Backend varre o PostgreSQL agregando todos os registos críticos, avisos, injeções de falha e leituras de sensores ocorridas na sessão atual.
5. O Backend empacota os dados e devolve-os no formato `.CSV` ou `.PDF`.
6. O navegador do utilizador inicia o download do ficheiro de auditoria.

## 5. Fluxos Alternativos
- **5a. Limpeza da Caixa Preta:**
  1. Após exportar com sucesso, um administrador com privilégios seleciona a opção "Puxar Data Recorder e Limpar Sessão".
  2. O backend confirma a exportação e limpa a base de dados (tabelas de sessão temporárias) para um novo ensaio de voo.

## 6. Pós-condições
- Um ficheiro imutável e estruturado fica disponível na máquina local do utilizador para análise formal dos tempos e comportamentos do sistema.

---

# CDU-08: Health Check e Topologia de Rede (Microsserviços)

## 1. Descrição
Monitoriza e exibe a "saúde" da própria arquitetura de software, garantindo que nenhum produtor, consumidor ou infraestrutura base (Kafka/PostgreSQL) caiu silenciosamente.

## 2. Atores
- **Utilizador:** Administrador do Sistema / Desenvolvedor.

## 3. Précondições
- A aplicação principal está a correr. Todos os contentores Docker (base de dados, Kafka, frontend, backend) devem estar operacionais.

## 4. Fluxo Principal
1. O utilizador acede ao separador **"Estado da Rede / Health Check"**.
2. O Vue renderiza um mapa lógico ou uma lista com os nós do sistema:
   - Barramento Kafka
   - PostgreSQL
   - Backend Spring Boot
   - Produtor FMS
   - Produtor Motor A, B, C
   - Gateway WAIC
3. O Backend fornece estas informações através de um *endpoint* dedicado (ex: Spring Boot Actuator ou uma tabela própria de *heartbeats* dos sensores).
4. O sistema exibe um marcador verde ("UP") ou vermelho ("DOWN") ao lado de cada módulo, além da latência (ping temporal) da sua última mensagem publicada.

## 5. Fluxos Alternativos
- **5a. Queda de um Componente Crítico:**
  1. O nó do PostgreSQL fica inoperante ou o broker Kafka cai.
  2. O Frontend deteta a falta de respostas de health check do Backend.
  3. O ecrã bloqueia funcionalidades que dependem de dados em tempo real e exibe um modal central a indicar perda catastrófica de comunicação de rede.

## 6. Pós-condições
- Transparência total sobre a infraestrutura distribuída, permitindo o reinício rápido de processos bloqueados sem necessidade de abrir terminais Docker no servidor.
