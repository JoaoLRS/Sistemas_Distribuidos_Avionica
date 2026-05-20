import json
import time
import random
import paho.mqtt.client as mqtt

BROKER = "broker.hivemq.com"
PORTA = 1883
TOPICO_RADAR = "avionica/radar"

def iniciar_radar():
    cliente = mqtt.Client()
    cliente.connect(BROKER, PORTA, 60)
    print("📡 Radar Meteorológico e ATC INICIADOS.")
    print("A varrer o espaço aéreo e a comunicar com as torres de controlo...")
    print("-" * 50)

    try:
        while True:
            pacote = {
                "origem": "Radar_ATC",
                "dados": {
                    "vento_knots": random.randint(5, 50),
                    "temp_externa_c": random.randint(-55, -40), # Frio extremo em altitude de cruzeiro
                    "turbulencia": random.choice(["NENHUMA", "LEVE", "MODERADA"]),
                    "gelo": random.choice(["LIVRE", "LIVRE", "AVISO: DETETADO"]), # Menor probabilidade de gelo
                    "radar_clima": random.choice(["CÉU LIMPO", "NUVENS DENSAS", "TEMPESTADE À FRENTE"]),
                    "qnh_hpa": random.choice([1012, 1013, 1014]), # Pressão atmosférica de referência
                    "atc_msg": random.choice(["MANTENHA FL350", "AUTORIZADO DESVIO DE ROTA", "CONTATE CENTRO", "PISTA LIVRE NO DESTINO"])
                }
            }
            cliente.publish(TOPICO_RADAR, json.dumps(pacote))
            print(f"🌩️ Ambiente | Clima: {pacote['dados']['radar_clima']} | ATC: {pacote['dados']['atc_msg']}")
            time.sleep(4) 
            
    except KeyboardInterrupt:
        print("\nRadar Desligado.")
        cliente.disconnect()

if __name__ == "__main__":
    iniciar_radar()