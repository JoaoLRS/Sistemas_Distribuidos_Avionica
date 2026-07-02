import os
import csv
import json
import time
import socket
import select
import threading
import argparse
from datetime import datetime

try:
    from kafka import KafkaConsumer, KafkaProducer
    _kafka_import_error = None
except Exception as e:
    KafkaConsumer = None
    KafkaProducer = None
    _kafka_import_error = e

# --- Configuration ---
KAFKA_BROKER = os.getenv("KAFKA_BROKER", "kafka:9092")
ROLE = os.getenv("ROLE", "master").lower() # 'master' ou 'slave'
SLAVE_HOST = os.getenv("SLAVE_HOST", "caixa-preta-slave")
SLAVE_PORT = int(os.getenv("SLAVE_PORT", "8500"))

TOPICS_TO_MONITOR = [
    "avionica.telemetry.motor.consolidated",
    "avionica.system.events"
]

# --- State ---
socket_to_slave = None
slave_connected = False

def create_kafka_producer():
    if KafkaProducer is None:
        print("[CAIXA-PRETA] Warning: kafka-python not available. Running without alert producer.")
        return None
    while True:
        try:
            producer = KafkaProducer(
                bootstrap_servers=KAFKA_BROKER,
                value_serializer=lambda v: json.dumps(v).encode('utf-8')
            )
            print("[CAIXA-PRETA] Conexão com o Kafka estabelecida (Produtor de Alertas).")
            return producer
        except Exception as e:
            print(f"[CAIXA-PRETA] Falha ao conectar ao Kafka como produtor: {e}. Retrying in 5 seconds...")
            time.sleep(5)

def create_kafka_consumer():
    if KafkaConsumer is None:
        print("[CAIXA-PRETA] Warning: kafka-python not available. Cannot consume Kafka.")
        return None
    while True:
        try:
            consumer = KafkaConsumer(
                bootstrap_servers=KAFKA_BROKER,
                auto_offset_reset='latest',
                group_id='caixa-preta-master-group',
                value_deserializer=lambda v: json.loads(v.decode('utf-8')) if v else None,
                consumer_timeout_ms=1000
            )
            print("[CAIXA-PRETA] Conexão com o Kafka estabelecida (Consumidor).")
            return consumer
        except Exception as e:
            print(f"[CAIXA-PRETA] Falha ao conectar ao Kafka como consumidor: {e}. Retrying in 5 seconds...")
            time.sleep(5)

def ensure_csv_file(filename):
    if not os.path.exists(filename):
        with open(filename, mode='w', newline='', encoding='utf-8') as file:
            writer = csv.writer(file, delimiter=';')
            writer.writerow(["Timestamp", "Topico", "Dados_JSON"])

def write_to_csv(filename, topic, payload_dict):
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S.%f")[:-3]
    try:
        payload_str = json.dumps(payload_dict)
    except Exception:
        payload_str = str(payload_dict)

    with open(filename, mode='a', newline='', encoding='utf-8') as file:
        writer = csv.writer(file, delimiter=';')
        writer.writerow([timestamp, topic, payload_str])
    print(f"[{timestamp}] 💾 GRAVADO ({ROLE.upper()}) -> {topic}")

# --- SLAVE SOCKET SERVER ---
def run_slave_server():
    print(f"[SLAVE] Iniciando servidor de réplica na porta {SLAVE_PORT}...")
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server_socket.bind(('0.0.0.0', SLAVE_PORT))
    server_socket.listen(5)
    
    filename = "flight_data_recorder_slave.csv"
    ensure_csv_file(filename)

    while True:
        try:
            client_conn, client_addr = server_socket.accept()
            print(f"[SLAVE] Conectado por Master em {client_addr}")
            client_conn.setblocking(True)
            buffer = ""
            
            while True:
                data = client_conn.recv(4096).decode('utf-8')
                if not data:
                    print("[SLAVE] Master desconectou.")
                    break
                buffer += data
                while "\n" in buffer:
                    line, buffer = buffer.split("\n", 1)
                    if not line.strip():
                        continue
                    try:
                        record = json.loads(line)
                        topic = record.get("topic", "unknown")
                        payload = record.get("payload", {})
                        
                        # Persiste na réplica
                        write_to_csv(filename, topic, payload)
                        
                        # Envia ACK síncrono
                        client_conn.sendall(b"ACK\n")
                    except Exception as e:
                        print(f"[SLAVE] Erro ao processar replicação: {e}")
                        client_conn.sendall(b"ERROR\n")
            client_conn.close()
        except Exception as e:
            print(f"[SLAVE] Erro no loop de conexões: {e}")
            time.sleep(2)

# --- MASTER SOCKET REPLICATION ---
def connect_to_slave():
    global socket_to_slave, slave_connected
    if slave_connected:
        return True
    
    try:
        socket_to_slave = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        socket_to_slave.settimeout(2.0)
        socket_to_slave.connect((SLAVE_HOST, SLAVE_PORT))
        slave_connected = True
        print(f"[MASTER] Conectado ao Slave Replica em {SLAVE_HOST}:{SLAVE_PORT}")
        return True
    except Exception as e:
        slave_connected = False
        socket_to_slave = None
        return False

def replicate_to_slave(topic, payload, alert_producer):
    global socket_to_slave, slave_connected
    if not connect_to_slave():
        print(f"[MASTER] Slave offline. Replicação ignorada temporariamente.")
        publish_replication_alert(alert_producer, "Falha de conexão com o Slave de réplica da Caixa Preta.")
        return False

    try:
        record = {"topic": topic, "payload": payload}
        message_str = json.dumps(record) + "\n"
        socket_to_slave.sendall(message_str.encode('utf-8'))
        
        # Espera pelo ACK com timeout de 3s usando select
        ready = select.select([socket_to_slave], [], [], 3.0)
        if ready[0]:
            response = socket_to_slave.recv(1024).decode('utf-8').strip()
            if "ACK" in response:
                print(f"[MASTER] Replicação confirmada pelo Slave (Quorum OK).")
                return True
            else:
                print(f"[MASTER] Erro retornado pelo Slave: {response}")
                return False
        else:
            print("[MASTER] Timeout aguardando ACK do Slave!")
            slave_connected = False
            socket_to_slave.close()
            socket_to_slave = None
            publish_replication_alert(alert_producer, "Timeout de 3s aguardando ACK da réplica da Caixa Preta.")
            return False
    except Exception as e:
        print(f"[MASTER] Erro de comunicação com o Slave: {e}")
        slave_connected = False
        if socket_to_slave:
            socket_to_slave.close()
        socket_to_slave = None
        publish_replication_alert(alert_producer, f"Erro de socket na replicação da Caixa Preta: {str(e)}")
        return False

def publish_replication_alert(producer, reason):
    if producer is None: return
    try:
        alert = {
            "tipo": "FALHA_REPLICACAO",
            "descricao": f"Erro de Quorum/Replicação na Caixa Preta: {reason}",
            "severidade": "WARNING",
            "origem": "CaixaPretaMaster",
            "resolvido": False
        }
        producer.send("avionica.system.events", alert)
        producer.flush()
        print(f"[MASTER] Alerta de Falha de Replicação enviado ao Kafka.")
    except Exception as err:
        print(f"[MASTER] Erro ao enviar alerta ao Kafka: {err}")

# --- MAIN EXECUTION ---
def main():
    parser = argparse.ArgumentParser(description='Caixa Preta Replicada (FDR)')
    parser.add_argument('--role', type=str, choices=['master', 'slave'], default=ROLE, help='Papel do nó (master ou slave)')
    args = parser.parse_args()

    role_to_run = args.role.lower()

    if role_to_run == 'slave':
        run_slave_server()
    else:
        # Modo Master
        print("[MASTER] Iniciando Gravador de Voo Master...")
        filename = "flight_data_recorder_master.csv"
        ensure_csv_file(filename)

        alert_producer = create_kafka_producer()
        consumer = create_kafka_consumer()
        if consumer is None:
            print("[MASTER] Erro: Não foi possível conectar ao Kafka. Encerrando.")
            return

        consumer.subscribe(TOPICS_TO_MONITOR)

        try:
            while True:
                records = consumer.poll(timeout_ms=1000)
                for topic_partition, messages in records.items():
                    for message in messages:
                        topic = message.topic
                        payload = message.value
                        
                        # Grava localmente no CSV
                        write_to_csv(filename, topic, payload)
                        
                        # Replica de forma síncrona com Quorum para o Slave
                        replicate_to_slave(topic, payload, alert_producer)
        except KeyboardInterrupt:
            print("[MASTER] Desligando Caixa Preta Master...")
        finally:
            if consumer:
                consumer.close()
            if alert_producer:
                alert_producer.close()
            if socket_to_slave:
                socket_to_slave.close()

if __name__ == "__main__":
    main()
