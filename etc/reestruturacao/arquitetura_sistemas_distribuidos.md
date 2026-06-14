# Arquitetura de Sistemas Distribuídos e Algoritmos Avançados

Para atender ao feedback do professor e evitar que o projeto seja considerado um simples sistema CRUD (leitura e escrita repetitiva em banco de dados), esta reestruturação adiciona **algoritmos e conceitos fundamentais de Sistemas Distribuídos** nos módulos da equipe. 

Cada integrante agora liderará a implementação de recursos que demonstram tolerância a falhas, consenso, sincronização ou replicação.

---

## 1. Algoritmos de Sistemas Distribuídos Injetados

### A. Consenso e Tolerância a Falhas: Redundância Modular Tripla (TMR)
* **O que resolve:** Evita que a leitura errada de um único sensor de motor corrompa os dados exibidos ao piloto.
* **Algoritmo:** São executados **3 processos de Sensores de Motor independentes** (Motor A, Motor B, Motor C). Um módulo centralizador (**Voter / Consensual Engine Monitor**) consome as três leituras simultaneamente e aplica uma votação por maioria (TMR). Se um dos motores apresentar temperatura/pressão divergente devido a uma falha injetada, o Voter a descarta e consolida o valor correto no barramento.
* **Demonstração prática:** Injetar uma falha no "Motor B" (fazendo-o enviar valores absurdos) e mostrar no dashboard do SGCA que o sistema ignora o erro e mantém o voo estável usando as leituras do Motor A e C.

### B. Eleição de Líder: Algoritmo do Valente (Bully Algorithm)
* **O que resolve:** Garante alta disponibilidade para o processamento de voo. Se o Computador de Navegação principal cair, outro assume de imediato.
* **Algoritmo:** Rodamos **2 a 3 instâncias** do Computador de Navegação (Primário, Secundário, Terciário) em containers separados. Eles trocam mensagens de sinal de vida (Heartbeats) entre si. Se o Primário cair (simulado por um `docker stop`), os demais detectam a ausência de ping e iniciam o **Bully Algorithm** por meio de sockets ou tópicos de eleição para determinar o novo Líder que assumirá a publicação no Kafka.
* **Demonstração prática:** Parar o container do Computador Primário ao vivo e ver no painel que o Computador Secundário assumiu a liderança em poucos segundos.

### C. Sincronização de Tempo: Algoritmo de Cristian e Lamport Timestamps
* **O que resolve:** Mensagens enviadas pela rede podem chegar fora de ordem no banco de dados devido à latência variável da rede.
* **Algoritmo:** 
    - **Algoritmo de Cristian:** Um serviço centralizador do Backend atua como servidor de tempo. Os sensores calibram seus relógios locais compensando o tempo de ida e volta (RTT) da rede.
    - **Lamport Timestamps:** Cada mensagem de telemetria ou evento possui um relógio lógico de Lamport incremental. O módulo de persistência ordena as mensagens na fila com base no relógio lógico e não no timestamp de recepção do banco.
* **Demonstração prática:** Exibição do atraso de rede (RTT) estimado de cada sensor na tela e ordenação cronológica lógica dos logs na Caixa Preta (FDR).

### D. Exclusão Mútua Distribuída (Token Ring ou Ricart-Agrawala)
* **O que resolve:** Dois sistemas automáticos (ex: Piloto Automático e Sistema de Pouso de Emergência) não podem atuar no controle do avião ou acionar o trem de pouso ao mesmo tempo sem coordenação.
* **Algoritmo:** Implementação de um algoritmo de exclusão mútua distribuída para acesso a recursos críticos (ex: Atuador do Trem de Pouso). O módulo só pode acionar o atuador se obtiver o "Token" distribuído ou aprovação da maioria dos nós consultados.
* **Demonstração prática:** Tentar acionar dois comandos concorrentes e exibir visualmente a fila de espera do token de exclusão mútua.

### E. Replicação de Dados Geográfica e Gossip Protocol
* **O que resolve:** Se o banco de dados principal de um aeroporto cair, os dados climáticos do radar devem continuar disponíveis.
* **Algoritmo:** Dois processos de radar em execução sincronizam suas tabelas climáticas internas usando um Gossip Protocol simplificado (comunicação par a par assíncrona periódica), garantindo consistência eventual entre eles.

---

## 2. Nova Divisão de Módulos com Foco em Algoritmos

Esta divisão remapeia as responsabilidades para que cada participante tenha em seus módulos uma lógica clara baseada nos algoritmos descritos:

| Integrante | Módulo 1 (Processo Principal) | Módulo 2 (Algoritmo de Sistemas Distribuídos) |
|---|---|---|
| **Gabriela** | **Voter Consensual do Motor**: Consome dados dos 3 sensores de motor e aplica votação TMR. | **Detector de Falhas (Heartbeats)**: Implementa detecção dinâmica de queda de nós. |
| **Rafael** | **FMS de Rotas**: Calcula caminhos, com algoritmo Dijkstra local de fallback em caso de falha da API externa. | **Radar Climático A (Gossip)**: Simula o radar e replica mapas com o Radar B via Gossip Protocol. |
| **Joao Lucas C.** | **Três Motores Redundantes (A, B, C)**: Simula os 3 processos independentes dos sensores do motor. | **Injetor de Falhas Distribuídas**: Painel que corrompe mensagens ou derruba nós para testar o TMR e Eleição. |
| **Mariana** | **Backend Gateway tolerante a falhas**: Gateway REST/SSE com Circuit Breaker ativo para APIs lentas. | **Servidor de Sincronização (Cristian)**: Servidor de hora que sincroniza os relógios dos sensores por RTT. |
| **Nickolas** | **Persistência em Fila por Relógio Lógico**: Ordena os eventos no banco usando Lamport Timestamps. | **Sensor de Freios com Exclusão Mútua**: Controla a ativação usando o algoritmo Ricart-Agrawala. |
| **Rafaely** | **Frontend Cockpit (SGCA)**: Exibe estados visuais dos consensos, computadores ativos e latências. | **Black Box / FDR Replicada**: Grava logs replicados em dois discos virtuais com confirmação de escrita. |
| **Alison** | **Computador de Voo Primário**: Computador de processamento de voo com suporte a failover e Bully Algorithm. | **Sensor de Altitude**: Sensor físico que realiza sincronização de relógio de Cristian antes de enviar dados. |
| **Joao Lucas R.** | **Computador de Voo Secundário**: Computador reserva que entra em eleição (Bully) se o primário cair. | **Sensor de Velocidade**: Sensor físico com compensação de atraso de rede. |
| **Ana Luisa** | **Sistema de Alertas (CAS)**: Consome eventos prioritários respeitando a ordem lógica dos timestamps. | **Radar Climático B (Gossip)**: Pareia com o Radar A para replicação eventual de informações de vento e gelo. |
