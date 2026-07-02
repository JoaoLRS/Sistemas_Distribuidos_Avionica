import json
import os
import time
import random
import paho.mqtt.client as mqtt

BROKER = os.getenv("MQTT_BROKER", "broker.hivemq.com")
PORTA = int(os.getenv("MQTT_PORT", "1883"))
TOPICO_RADAR = "avionica/radar"
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

def iniciar_radar():
    global simulacao_ativa
    cliente = mqtt.Client()
    cliente.on_connect = ao_conectar
    cliente.on_message = ao_receber_mensagem
    cliente.connect(BROKER, PORTA, 60)
    cliente.loop_start()

    climas = ["CÉU LIMPO", "NUVENS", "TEMPESTADE"]

    try:
        while True:
            if not simulacao_ativa:
                time.sleep(1)
                continue

            clima_atual = random.choices(climas, weights=[50, 30, 20])[0]
            # Se for tempestade, a temperatura cai drasticamente para baixo de zero
            temp = random.randint(-30, -5) if clima_atual == "TEMPESTADE" else random.randint(5, 15)

            pacote = {
                "id_mensagem": f"rad_{int(time.time()*1000)}",
                "dados": {
                    "vento_knots": random.randint(0, 40),
                    "turbulencia": "SEVERA" if clima_atual == "TEMPESTADE" else "LEVE",
                    "radar_clima": clima_atual,
                    "temp_externa_c": temp,
                    "qnh_hpa": random.randint(1000, 1025),
                    "atc_msg": "Evite formacoes" if clima_atual == "TEMPESTADE" else "Rota livre"
                }
            }
            cliente.publish(TOPICO_RADAR, json.dumps(pacote))
            time.sleep(3) # O radar varre a cada 3 segundos
    except KeyboardInterrupt:
        cliente.loop_stop()
        cliente.disconnect()

if __name__ == "__main__":
    iniciar_radar()
