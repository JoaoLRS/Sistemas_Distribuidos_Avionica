import json
import time
import threading
from kafka import KafkaConsumer, KafkaProducer

# Configurações do Kafka
KAFKA_BROKER = 'localhost:9092'
TOPIC_MOTOR_A = 'avionica.telemetry.motor.a'
TOPIC_MOTOR_B = 'avionica.telemetry.motor.b'
TOPIC_MOTOR_C = 'avionica.telemetry.motor.c'
TOPIC_CONSOLIDATED = 'avionica.telemetry.motor.consolidated'
TOPIC_EVENTS = 'avionica.system.events'

class MotorVoter:
    def __init__(self):
        # Buffer para armazenar a última leitura de cada motor
        self.buffer = {'a': None, 'b': None, 'c': None}
        self.lock = threading.Lock()
        
        # Produtor Kafka para enviar os resultados e eventos
        self.producer = KafkaProducer(
            bootstrap_servers=KAFKA_BROKER,
            value_serializer=lambda v: json.dumps(v).encode('utf-8')
        )

    def iniciar_consumidores(self):
        """Inicia os consumidores para os três tópicos dos motores."""
        topics = [TOPIC_MOTOR_A, TOPIC_MOTOR_B, TOPIC_MOTOR_C]
        consumer = KafkaConsumer(
            *topics,
            bootstrap_servers=KAFKA_BROKER,
            value_deserializer=lambda m: json.loads(m.decode('utf-8'))
        )

        print("[TMR VOTER] Aguardando leituras dos motores A, B e C...")
        for message in consumer:
            topic = message.topic
            payload = message.value
            
            with self.lock:
                if 'motor.a' in topic:
                    self.buffer['a'] = payload
                elif 'motor.b' in topic:
                    self.buffer['b'] = payload
                elif 'motor.c' in topic:
                    self.buffer['c'] = payload
                
                # Se temos leituras frescas dos três motores, iniciamos a votação
                if all(self.buffer.values()):
                    self.realizar_votacao()
                    # Limpa o buffer para a próxima rodada
                    self.buffer = {'a': None, 'b': None, 'c': None}

    def calcular_divergencia(self, val1, val2):
        """Calcula a variação percentual entre dois valores."""
        if max(val1, val2) == 0:
            return 0
        return abs(val1 - val2) / max(abs(val1), abs(val2))

    def avaliar_sensores(self, vals):
        """
        Aplica a lógica TMR (Triple Modular Redundancy).
        Retorna uma tupla (lista_validos, motor_falho, falha_bizantina)
        """
        a, b, c = vals['a'], vals['b'], vals['c']
        
        div_ab = self.calcular_divergencia(a, b) <= 0.05
        div_bc = self.calcular_divergencia(b, c) <= 0.05
        div_ac = self.calcular_divergencia(a, c) <= 0.05

        if div_ab and div_bc and div_ac:
            return (['a', 'b', 'c'], None, False) # Todos coerentes
        elif div_ab and not div_bc and not div_ac:
            return (['a', 'b'], 'c', False)       # C divergente
        elif div_ac and not div_ab and not div_bc:
            return (['a', 'c'], 'b', False)       # B divergente
        elif div_bc and not div_ab and not div_ac:
            return (['b', 'c'], 'a', False)       # A divergente
        else:
            return ([], None, True)               # Falha Bizantina (nenhum concorda)

    def realizar_votacao(self):
        """Compara os valores, gera média e identifica falhas."""
        temperaturas = {k: v['temperatura'] for k, v in self.buffer.items()}
        pressoes = {k: v['pressao'] for k, v in self.buffer.items()}

        validos_temp, falho_temp, bizantino_temp = self.avaliar_sensores(temperaturas)
        validos_press, falho_press, bizantino_press = self.avaliar_sensores(pressoes)

        # Se houver falha bizantina em qualquer parâmetro, envia alerta crítico
        if bizantino_temp or bizantino_press:
            print("[CRITICAL] Falha Bizantina detectada! Sensores totalmente divergentes.")
            evento = {"severidade": "CRITICAL", "mensagem": "Falha bizantina nos motores insolúvel."}
            self.producer.send(TOPIC_EVENTS, evento)
            return

        # Um motor é considerado falho se falhar em QUALQUER uma das leituras
        motor_falho = falho_temp or falho_press
        
        # Motores que sobreviveram à votação
        motores_validos = [m for m in ['a', 'b', 'c'] if m != motor_falho]

        if motor_falho:
            alerta = f"Sensor do Motor {motor_falho.upper()} falhou - valor discrepante descartado."
            print(f"[WARNING] {alerta}")
            self.producer.send(TOPIC_EVENTS, {"severidade": "WARNING", "mensagem": alerta})

        # Calcula a média aritmética simples dos motores válidos
        media_temp = sum(temperaturas[m] for m in motores_validos) / len(motores_validos)
        media_press = sum(pressoes[m] for m in motores_validos) / len(motores_validos)

        payload_consolidado = {
            "timestamp": time.time(),
            "temperatura_consolidada": round(media_temp, 2),
            "pressao_consolidada": round(media_press, 2),
            "motores_utilizados": len(motores_validos)
        }

        print(f"[PRODUÇÃO] Telemetria Consolidada: {payload_consolidado}")
        self.producer.send(TOPIC_CONSOLIDATED, payload_consolidado)

if __name__ == "__main__":
    voter = MotorVoter()
    voter.iniciar_consumidores()