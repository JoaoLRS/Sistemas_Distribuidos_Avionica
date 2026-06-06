# CDU-04: Consulta de Histórico e Logs de Mensagens

## 1. Descrição
Permite que o usuário consulte os registros históricos (logs brutos) armazenados no banco de dados, visualizando tudo que passou pelo barramento de mensagens.

## 2. Atores
- **Usuário:** Engenheiro ou mantenedor do sistema auditando eventos passados.

## 3. Pré-condições
- O Backend está gravando as mensagens no banco de dados PostgreSQL.

## 4. Fluxo Principal
1. O usuário acessa a aba/menu **"Histórico / Log de Mensagens"**.
2. O sistema exibe uma interface de auditoria com filtros: **Data Inicial/Final**, **Tópico MQTT/Kafka**, e **Texto/Payload**.
3. O usuário insere parâmetros de busca (ex: filtrar apenas mensagens do tópico `avionica/sensores/waic` das últimas 2 horas) e clica em **"Buscar"**.
4. O Frontend faz uma requisição HTTP GET para o Backend com os parâmetros na *query string*.
5. O Backend executa uma consulta paginada no PostgreSQL (tabela de `mensagens_barramento` ou equivalente).
6. O Frontend exibe os resultados em uma tabela com suporte a paginação, mostrando a data exata da captura, o tópico e o payload JSON bruto da mensagem.
7. O usuário pode clicar sobre uma mensagem específica para expandir e ler o JSON formatado.

## 5. Fluxos Alternativos
- **5a. Busca Sem Resultados:**
  1. No Passo 5, o banco não encontra nenhuma mensagem com os critérios fornecidos.
  2. O Frontend recebe uma lista vazia.
  3. O sistema exibe uma mensagem: *"Nenhuma mensagem encontrada para o filtro selecionado."*

## 6. Pós-condições
- O usuário consegue auditar os dados distribuídos sem precisar conectar diretamente no banco de dados, confirmando a rastreabilidade do sistema.
