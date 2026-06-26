# Tarefas de Execução — João Lucas Cosme

Você é responsável pelo desenvolvimento e integração de **dois módulos distribuídos** com foco em Resiliência de API e Replicação de Dados Geográfica.

---

## Módulo 1: FMS de Planejamento de Rotas (API Fallback)

### Objetivo
Lidar com o cálculo de caminhos e garantir o funcionamento do sistema mesmo quando o acesso à internet ou à API de aeroportos externa falhar.

### Especificação Técnica
- **Tecnologia:** Python (pode alterar/adaptar o arquivo `fms_distribuido.py` existente).
- **Consumo:** Escutar o tópico Kafka `avionica.route.requested`.
- **Lógica (Fallback com Dijkstra/A* Local):**
  1. Ao receber a solicitação com `callsign`, `origem` e `destino`.
  2. Tentar efetuar a requisição HTTP para a API externa (Ninjas API).
  3. Se a requisição HTTP falhar (erro 5xx, timeout ou falta de internet), **ativar o mecanismo de fallback**:
     - Carregar um arquivo JSON local contendo coordenadas geográficas pré-definidas dos principais aeroportos brasileiros (ex: SBGR, SBRJ, SBSP, SBGL, SBFZ).
     - Aplicar o algoritmo de Dijkstra ou cálculo de distância de Haversine para encontrar a menor distância e o ETA estimado.
  4. Marcar no payload de saída se a rota foi calculada via `ONLINE` ou em modo degradado `OFFLINE_FALLBACK`.
- **Produção:**
  - Publicar a rota no tópico Kafka `avionica.route.calculated` (JSON contendo `callsign`, `origem`, `destino`, `distancia_nm`, `eta_minutos`, `status` [ONLINE/FALLBACK]).

---

## Módulo 2: Radar Climático A (Gossip Protocol)

### Objetivo
Replicar de forma descentralizada as condições meteorológicas detectadas em diferentes regiões geográficas, garantindo tolerância a partições de rede.

### Especificação Técnica
- **Tecnologia:** Python.
- **Integração Peer-to-Peer:**
  1. O Radar Climático A roda em uma porta de rede e o Radar Climático B (Módulo do Alison) roda em outra.
  2. Estabelecer uma comunicação peer-to-peer (sockets TCP ou endpoints HTTP leves).
  3. A cada 10 segundos, o Radar A gera uma leitura climática (vento, tempestade, temperatura).
  4. Utilizar um **Gossip Protocol** simples: o Radar A envia suas últimas atualizações climáticas para o Radar B e recebe dele as informações da outra região geográfica.
  5. Consolidar os dados em uma tabela climática local mantendo consistência eventual entre ambos os nós.
- **Produção:**
  - Publicar a telemetria climática no tópico Kafka `avionica.telemetry.radar`.
