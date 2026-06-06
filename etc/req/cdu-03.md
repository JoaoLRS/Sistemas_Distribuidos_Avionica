# CDU-03: Visualização e Injeção de Falhas

## 1. Descrição
Permite ao usuário acompanhar alertas e anomalias do sistema em tempo real, bem como injetar falhas propositais (simulação) para validar o comportamento dos computadores de automação.

## 2. Atores
- **Usuário:** Operador simulando falhas ou analisando anomalias.
- **Injetor de Falhas:** Módulo encarregado de gerar eventos anômalos no sistema.
- **Sistema de Alertas:** Lógica no Backend/Sensores que classifica a severidade dos eventos.

## 3. Pré-condições
- O sistema está monitorando os sensores ativamente.

## 4. Fluxo Principal (Visualização de Falhas)
1. O usuário acessa a aba **"Central de Alertas e Falhas"** no Frontend.
2. O sistema exibe uma tabela de alertas contendo: Data/Hora, Origem (qual sensor), Tipo de Falha, Descrição e Severidade (INFO, WARNING, CRITICAL).
3. À medida que os sensores reportam leituras anormais (ex: pressão do motor subindo excessivamente ou gelo detectado pelo radar), novos alertas aparecem no topo da tabela automaticamente.
4. O usuário pode clicar em um botão **"Marcar como Resolvido"** ao lado de falhas antigas para limpar o histórico visual (opcional).

## 5. Fluxo Principal (Injeção de Falhas - Modo Simulação)
1. Na mesma tela, o usuário localiza o painel de **"Injeção de Falhas"**.
2. O usuário seleciona no menu *dropdown* o componente que deseja comprometer (Ex: Sensor de Voo) e o tipo de erro (Ex: *Falha no tubo de Pitot / Velocidade a 0*).
3. O usuário clica em **"Injetar Falha"**.
4. O Frontend envia o comando para o Backend, que repassa a instrução via Kafka/MQTT para o simulador do respectivo sensor.
5. O sensor afetado passa a publicar dados anômalos no barramento (ex: reportando Mach 0.0 de forma repentina).
6. O Computador de Automação ou o Sistema de Alertas captura a discrepância e gera um aviso de Severidade CRITICAL.
7. O alerta aparece em destaque (ex: cor vermelha e aviso sonoro) no Dashboard e na Central de Alertas.

## 6. Fluxos Alternativos
- **6a. Falha Isolada de Conexão:**
  1. Se um alerta for gerado simplesmente pela queda de conexão de um sensor (timeout), ele recebe a classificação WARNING.
  2. O usuário tenta injetar uma falha num sensor que já está offline; o sistema apenas ignora o comando.

## 7. Pós-condições
- Os operadores de teste conseguem validar se o barramento distribuído e os painéis web estão reagindo a anomalias conforme esperado.
