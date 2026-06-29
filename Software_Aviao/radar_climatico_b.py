import json
import os
import time
import random
import socket
import threading
import paho.mqtt.client as mqtt

# Configurações do Broker MQTT (Produção)
BROKER = os.getenv("MQTT_BROKER", "broker.hivemq.com")
PORTA = int(os.getenv("MQTT_PORT", "1883"))
TOPICO_TELEMETRIA = "avionica.telemetry.radar"

# Configurações de Rede P2P (Gossip Protocol via TCP)
TCP_MEU_HOST = "0.0.0.0"
TCP_MINHA_PORTA = int(os.getenv("RADAR_B_PORT", "5002")) # Porta do Radar B
TCP_PEER_HOST = os.getenv("RADAR_A_HOST", "127.0.0.1")  # IP do Radar A
TCP_PEER_PORT = int(os.getenv("RADAR_A_PORT", "5001")) # Porta do Radar A

class RadarClimaticoB:
    def __init__(self):
        # Malha climática local na memória (Armazena o último pacote válido de cada região)
        self.malha_climatica = {
            "Regiao_A": None,
            "Regiao_B": None
        }
        self.lock = threading.Lock()
        self.climas = ["CÉU LIMPO", "NUVENS", "TEMPESTADE"]

    def extrair_timestamp(self, pacote):
        """Extrai o timestamp numérico a partir do id_mensagem (ex: rad_1719600000000)"""
        if not pacote or "id_mensagem" not in pacote:
            return 0
        try:
            return int(pacote["id_mensagem"].split("_")[1])
        except Exception:
            return 0

    def mesclar_dados(self, regiao, novo_pacote):
        """Mescla os dados na memória aplicando Consistência Eventual baseada em Timestamps"""
        if not novo_pacote:
            return

        with self.lock:
            pacote_atual = self.malha_climatica[regiao]
            ts_atual = self.extrair_timestamp(pacote_atual)
            ts_novo = self.extrair_timestamp(novo_pacote)

            # O dado mais recente sempre substitui o antigo
            if ts_novo > ts_atual:
                self.malha_climatica[regiao] = novo_pacote
                print(f"[GOSSIP] Malha atualizada para {regiao}. ID: {novo_pacote['id_mensagem']}")

    def gerar_clima_local(self):
        """Loop idêntico ao Radar A para gerar os dados específicos da Região B"""
        print("[THREAD] Gerador de clima local da Região B iniciado.")
        while True:
            clima_atual = random.choices(self.climas, weights=[50, 30, 20])[0]
            temp = random.randint(-30, -5) if clima_atual == "TEMPESTADE" else random.randint(5, 15)

            pacote_local = {
                "id_mensagem": f"radB_{int(time.time()*1000)}",
                "dados": {
                    "vento_knots": random.randint(0, 40),
                    "turbulencia": "SEVERA" if clima_atual == "TEMPESTADE" else "LEVE",
                    "radar_clima": clima_atual,
                    "temp_externa_c": temp,
                    "qnh_hpa": random.randint(1000, 1025),
                    "atc_msg": "Evite formaacoes" if clima_atual == "TEMPESTADE" else "Rota livre"
                }
            }
            
            self.mesclar_dados("Regiao_B", pacote_local)
            time.sleep(3) # Varredura a cada 3 segundos

    def servidor_gossip(self):
        """Escuta conexões TCP recebidas do Radar A para trocar informações"""
        servidor = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        servidor.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        servidor.bind((TCP_MEU_HOST, TCP_MINHA_PORTA))
        servidor.listen(5)
        print(f"[TCP SERVER] Radar B aguardando fofocas na porta {TCP_MINHA_PORTA}...")

        while True:
            try:
                conn, _ = server.accept()
                dados_recebidos = conn.recv(4096)
                if dados_recebidos:
                    # Radar A enviou a malha dele
                    malha_externa = json.loads(dados_recebidos.decode('utf-8'))
                    
                    # Tenta mesclar o que o Radar A sabe sobre a Região A
                    if "Regiao_A" in malha_externa:
                        self.mesclar_dados("Regiao_A", malha_externa["Regiao_A"])

                    # Responde enviando de volta a malha atual do Radar B
                    with self.lock:
                        resposta = json.dumps(self.malha_climatica)
                    conn.sendall(resposta.encode('utf-8'))
                conn.close()
            except Exception as e:
                print(f"[ERRO SERVER] Falha ao processar requisição TCP: {e}")

    def cliente_gossip_loop(self):
        """Inicia a fofoca ativa com o Radar A a cada 10 segundos"""
        print(f"[GOSSIP CLIENT] Ativo. Tentando conectar ao Radar A ({TCP_PEER_HOST}:{TCP_PEER_PORT}) a cada 10s.")
        while True:
            time.sleep(10)
            try:
                cliente = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                cliente.settimeout(3.0)
                cliente.connect((TCP_PEER_HOST, TCP_PEER_PORT))

                # Envia nossa malha climática atual
                with self.lock:
                    payload = json.dumps(self.malha_climatica)
                cliente.sendall(payload.encode('utf-8'))

                # Recebe a malha do Radar A de volta
                resposta = cliente.recv(4096)
                if resposta:
                    malha_externa = json.loads(resposta.decode('utf-8'))
                    if "Regiao_A" in malha_externa:
                        self.mesclar_dados("Regiao_A", malha_externa["Regiao_A"])
                    print("[GOSSIP] Sincronização ativa concluída com sucesso.")
                
                cliente.close()
            except (ConnectionRefusedError, socket.timeout):
                print("[GOSSIP] Radar A offline ou inacessível. Mantendo dados em cache.")
            except Exception as e:
                print(f"[ERRO CLIENT] Falha no loop de Gossip: {e}")

    def publicar_producao_mqtt(self):
        """Publica a telemetria unificada (A + B) no broker MQTT"""
        cliente_mqtt = mqtt.Client()
        cliente_mqtt.connect(BROKER, PORTA, 60)
        cliente_mqtt.loop_start()
        print(f"[MQTT] Conectado e pronto para publicar em '{TOPIC_TELEMETRY}'")

        while True:
            with self.lock:
                # Só publica se já tivermos pelo menos o nosso próprio clima gerado
                if self.malha_climatica["Regiao_B"] is not None:
                    payload = json.dumps(self.malha_climatica)
                    cliente_mqtt.publish(TOPIC_TELEMETRY, payload)
                    print(f"[PRODUÇÃO MQTT] Malha Consolidada publicada.")
            time.sleep(3)

    def iniciar(self):
        # Disparando as 4 tarefas concorrentes usando Threads
        threading.Thread(target=self.gerar_clima_local, daemon=True).start()
        threading.Thread(target=self.servidor_gossip, daemon=True).start()
        threading.Thread(target=self.cliente_gossip_loop, daemon=True).start()
        
        # Inicia a publicação MQTT na thread principal
        try:
            self.publicar_producao_mqtt()
        except KeyboardInterrupt:
            print("\n[SHUTDOWN] Encerrando Radar B.")

if __name__ == "__main__":
    radar = RadarClimaticoB()
    radar.iniciar()
