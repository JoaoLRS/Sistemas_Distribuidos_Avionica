# ✈️ Protótipo de Sistema Distribuído Aviônico (AFDX/WAIC)

Este repositório contém o código-fonte de um protótipo de software crítico distribuído para aviação, desenvolvido como Prova de Conceito (PoC) para análise de redes AFDX (*Avionics Full-Duplex Switched Ethernet*) e WAIC (*Wireless Avionics Intra-Communications*).

O sistema abandona a arquitetura monolítica tradicional em favor de uma **Arquitetura de Microsserviços Orientada a Eventos**, utilizando o protocolo de mensageria **MQTT** (Publish/Subscribe) e orquestração via **Docker**.

## 🚀 Principais Funcionalidades

* **Comunicação Desacoplada (Pub/Sub):** Módulos comunicam-se de forma assíncrona, simulando o tráfego de *Virtual Links* numa rede AFDX.
* **Tolerância a Falhas Bizantinas (TMR):** Implementação de *Triple Modular Redundancy* nos sensores do motor. Um algoritmo Votador (Consenso) isola sensores corrompidos através do cálculo da mediana, garantindo que o painel do piloto nunca receba dados anómalos.
* **Coreografia de Microsserviços (Automação):** O sistema Anti-Gelo (FADEC) opera de forma reativa e autónoma, avaliando publicações do Radar Meteorológico e ativando as defesas da aeronave sem intervenção do piloto.
* **Auditoria e Registo (FDR - Caixa Preta):** Um nó de escuta passiva captura e persiste todo o tráfego da rede num ficheiro CSV (timestamp ao milissegundo) para análise *post-mortem*.
* **Interface Gráfica (Glass Cockpit):** Painel do piloto desenvolvido com `customtkinter`, exibindo telemetria em tempo real.
* **Injeção de Falhas (Simulador):** Painel do instrutor para forçar a corrupção de sensores e simular alertas (EGPWS, Engine Fire) em pleno voo.

---

## 📂 Estrutura dos Microsserviços

O backend da aeronave foi modelado em contentores independentes. Abaixo está a finalidade de cada script Python:

### Sensores e Módulos Físicos
* `sensor_freio.py`: Publica a temperatura dos travões e monitoriza o uso de *Autobrake*.
* `sensores_voo.py`: Publica a atitude da aeronave (Pitch, Roll) e os dados do Tubo de Pitot (Velocidade, Altitude).
* `radar_externo.py`: Gera condições meteorológicas aleatórias (Céu Limpo, Nuvens, Tempestade) e temperaturas externas associadas.
* `sensor_motor.py`: Script base instanciado 3 vezes (A, B e C) via Docker, simulando redundância tripla na leitura da pressão do motor (N1).

### Sistemas de Computação e Consenso
* `computador_navegacao.py`: Simula o sistema ILS/GPS, publicando desvios de rota.
* `fms_distribuido.py`: API REST (Flask) do Flight Management System, servindo o plano de voo em JSON.
* `consenso_motor.py`: O "Nó Votador". Ouve os três sensores de motor, descarta anomalias (Falha Bizantina) e publica a leitura de consenso segura.
* `computador_automacao.py`: O "Cérebro" autónomo. Ouve o Radar e, caso detete Tempestade e Temperatura < 0°C, comanda o sistema Anti-Gelo a ligar (Coreografia).

### Ferramentas de Superfície (Interfaces)
* `computador_voo.py`: O Frontend do Piloto (Glass Cockpit). Inscreve-se em todos os tópicos validados e exibe os dados visualmente.
* `injetor_falhas.py`: O Frontend do Instrutor. Publica comandos destrutivos num tópico restrito para corromper sensores específicos e avaliar a resposta da rede.
* `caixa_preta.py`: O gravador *Flight Data Recorder*. Usa o *wildcard* `#` do MQTT para ouvir toda a comunicação e salvar logs em `flight_data_recorder.csv`.

---

## 🛠️ Como Executar o Sistema

Pré-requisitos: Ter o **Docker Desktop** e o **Python 3.x** instalados. Recomenda-se a instalação das bibliotecas para os nós locais (`pip install paho-mqtt customtkinter requests`).

### Passo 1: Levantar a Infraestrutura (Backend)
Na raiz do projeto (onde está o ficheiro `docker-compose.yml`), abra o terminal e execute:
```bash
docker compose up --build -d
