# CDU-02: Gerenciamento de Rotas (FMS)

## 1. Descrição
Permite que o usuário insira as localidades de origem e destino da aeronave e acompanhe o cálculo de distância, a rota formatada e a estimativa de tempo de chegada fornecidos pelo Flight Management System (FMS).

## 2. Atores
- **Usuário:** Operador visualizando e inserindo dados de voo.
- **Módulo FMS:** Módulo Python responsável por calcular a rota conectando-se a APIs externas.

## 3. Pré-condições
- O sistema distribuído está rodando normalmente.
- O Frontend tem acesso à rota de API de gerenciamento de voos no Backend.

## 4. Fluxo Principal
1. O usuário acessa a aba/menu **"Navegação / FMS"** no Frontend.
2. O sistema exibe os campos de entrada de rota: **Código ICAO de Origem** (ex: SBGR) e **Código ICAO de Destino** (ex: EGLL), além de um botão **"Calcular Rota"**.
3. O usuário digita os códigos ICAO válidos e clica em "Calcular Rota".
4. O Frontend envia a requisição de nova rota para o Backend.
5. O Backend registra o pedido e encaminha o comando via Kafka/MQTT para o módulo FMS.
6. O módulo FMS consulta as coordenadas (usando API externa de aeroportos), calcula a distância e publica o resultado de volta no barramento.
7. O Backend captura o resultado e o salva no PostgreSQL.
8. A tela do Frontend é atualizada e passa a exibir:
   - Rota detalhada (Origem → Destino).
   - Distância em Milhas Náuticas (NM).
   - Tempo Estimado de Chegada (ETA).

## 5. Fluxos Alternativos
- **5a. Código ICAO Inválido:**
  1. No Passo 3, o usuário digita um código ICAO inexistente (ex: AAAA).
  2. O módulo FMS tenta calcular a rota e retorna um erro de localidade não encontrada.
  3. O Frontend recebe o erro e exibe uma notificação para o usuário: *"Código ICAO inválido ou aeroporto não encontrado."*
  4. Os campos continuam disponíveis para o usuário corrigir e tentar novamente.

## 6. Pós-condições
- A nova rota se torna a rota "ativa" no sistema e fica disponível para os outros módulos (como o Computador de Navegação).
