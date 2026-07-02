import json
import os
import time
import random
import paho.mqtt.client as mqtt

BROKER = os.getenv("MQTT_BROKER", "broker.hivemq.com")
PORTA = int(os.getenv("MQTT_PORT", "1883"))
TOPICO_NAV = "avionica/navegacao"
TOPICO_SIMULACAO = "avionica/comandos/simulacao"

simulacao_ativa = False

def ao_conectar(client, userdata, flags, rc):
    client.subscribe(TOPICO_SIMULACAO)

def ao_receber_mensagem(client, userdata, msg):
    global simulacao_ativa
    try:
        pacote = json.loads(msg.payload.decode("utf-8"))
        status = pacote.get("status")
        if status == "START":
            simulacao_ativa = True
        elif status == "STOP":
            simulacao_ativa = False
    except Exception:
        pass

def iniciar_navegacao():
    global simulacao_ativa
    cliente = mqtt.Client()
    cliente.on_connect = ao_conectar
    cliente.on_message = ao_receber_mensagem
    cliente.connect(BROKER, PORTA, 60)
    cliente.loop_start()
    
    print("🧭 Computador de Navegação e Automação INICIADO (Aguardando simulação...).")
    print("-" * 50)

    waypoints = ["LISBOA_FIR", "WAYPOINT_ALFA", "ROTA_OCEANICA", "APROXIMACAO_FINAL"]

    try:
        while True:
            if not simulacao_ativa:
                time.sleep(1)
                continue

            # Simulando os cálculos de navegação
            pacote = {
                "origem": "Computador_Nav",
                "dados": {
                    "proa_graus": random.randint(265, 275), # Voando para Oeste
                    "vs_fpm": random.randint(-150, 150),    # Razão de subida/descida (Pés por Minuto)
                    "piloto_automatico": "LIGADO (LNAV/VNAV)",
                    "waypoint_ativo": random.choice(waypoints),
                    "eta_minutos": random.randint(15, 120)
                }
            }
            cliente.publish(TOPICO_NAV, json.dumps(pacote))
            print(f"📍 Navegação | Proa: {pacote['dados']['proa_graus']}° | Rumo a: {pacote['dados']['waypoint_ativo']}")
            time.sleep(3) # Atualiza a cada 3 segundos
            
    except KeyboardInterrupt:
        print("\nNavegação Desligada.")
        cliente.loop_stop()
        cliente.disconnect()

if __name__ == "__main__":
    iniciar_navegacao()
