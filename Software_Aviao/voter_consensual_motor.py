import json
import os
import time
import threading
from datetime import datetime, timezone
from kafka import KafkaConsumer, KafkaProducer

# ── Configuração ──────────────────────────────────────────────
KAFKA_BROKER   = os.getenv("KAFKA_BROKER", "kafka:9092")
TOPIC_MOTOR_A  = "avionica.telemetry.motor.a"
TOPIC_MOTOR_B  = "avionica.telemetry.motor.b"
TOPIC_MOTOR_C  = "avionica.telemetry.motor.c"
TOPIC_CONSOLIDATED = "avionica.telemetry.motor.consolidated"
TOPIC_EVENTS   = "avionica.system.events"
TOPIC_HEALTH   = "avionica.module.health"

VARIACAO_LIMITE = 0.05  # 5%
INTERVALO_VOTACAO = 2   # segundos entre cada ciclo de votação

# ── Estado ────────────────────────────────────────────────────
leituras = {
    "A": None,
    "B": None,
    "C": None,
}

lock = threading.Lock()

# ── Funções utilitárias ───────────────────────────────────────

def variacao_relativa(a, b):
    """Calcula a variação relativa entre dois valores."""
    media = (a + b) / 2.0
    if media == 0:
        return 0.0
    return abs(a - b) / abs(media)


def extrair_grandezas(payload):
    """Extrai rpm, temperature, pressure de um payload."""
    dados = payload.get("dados", payload)
    return {
        "rpm": dados.get("rpm", 0),
        "temperature": dados.get("temperature", dados.get("temperatura", 0)),
        "pressure": dados.get("pressure", dados.get("pressao", 0)),
    }


def votar_grandeza(nome, val_a, val_b, val_c, producer):
    """
    Aplica TMR para uma grandeza específica.
    Retorna (valor_consensual, alertas_gerados).
    """
    alertas = []
    valores = {"A": val_a, "B": val_b, "C": val_c}

    # Verificar variação entre todos os pares
    var_ab = variacao_relativa(val_a, val_b)
    var_ac = variacao_relativa(val_a, val_c)
    var_bc = variacao_relativa(val_b, val_c)

    # Caso 1: Todos concordam (variação < 5%)
    if var_ab < VARIACAO_LIMITE and var_ac < VARIACAO_LIMITE and var_bc < VARIACAO_LIMITE:
        consenso = (val_a + val_b + val_c) / 3.0
        return round(consenso, 2), alertas

    # Caso 2: Um motor diverge (falha bizantina parcial)
    # Encontra o motor que diverge dos outros dois
    if var_bc < VARIACAO_LIMITE and (var_ab >= VARIACAO_LIMITE or var_ac >= VARIACAO_LIMITE):
        # Motor A diverge de B e C (que concordam)
        consenso = (val_b + val_c) / 2.0
        msg = f"Sensor do Motor A descartado por divergência (>5%) em {nome}. Valor: {val_a:.2f}, Consenso B+C: {consenso:.2f}"
        alertas.append(("WARNING", msg, "A"))
        return round(consenso, 2), alertas

    if var_ac < VARIACAO_LIMITE and (var_ab >= VARIACAO_LIMITE or var_bc >= VARIACAO_LIMITE):
        # Motor B diverge de A e C (que concordam)
        consenso = (val_a + val_c) / 2.0
        msg = f"Sensor do Motor B descartado por divergência (>5%) em {nome}. Valor: {val_b:.2f}, Consenso A+C: {consenso:.2f}"
        alertas.append(("WARNING", msg, "B"))
        return round(consenso, 2), alertas

    if var_ab < VARIACAO_LIMITE and (var_ac >= VARIACAO_LIMITE or var_bc >= VARIACAO_LIMITE):
        # Motor C diverge de A e B (que concordam)
        consenso = (val_a + val_b) / 2.0
        msg = f"Sensor do Motor C descartado por divergência (>5%) em {nome}. Valor: {val_c:.2f}, Consenso A+B: {consenso:.2f}"
        alertas.append(("WARNING", msg, "C"))
        return round(consenso, 2), alertas

    # Caso 3: Todos divergem (falha bizantina geral — CRÍTICO)
    consenso = sorted([val_a, val_b, val_c])[1]  # mediana como fallback
    msg = f"FALHA BIZANTINA GERAL em {nome}! Todos os 3 motores divergem (>5%). A={val_a:.2f}, B={val_b:.2f}, C={val_c:.2f}"
    alertas.append(("CRITICAL", msg, None))
    return round(consenso, 2), alertas


# ── Thread: Consumidor Kafka ──────────────────────────────────

def consumer_loop():
    """Consome telemetria dos 3 motores via Kafka."""
    print("[TMR-Voter] Conectando ao Kafka...", flush=True)
    
    while True:
        try:
            consumer = KafkaConsumer(
                TOPIC_MOTOR_A, TOPIC_MOTOR_B, TOPIC_MOTOR_C,
                bootstrap_servers=KAFKA_BROKER,
                value_deserializer=lambda m: json.loads(m.decode("utf-8")),
                auto_offset_reset="latest",
                group_id="tmr-voter-group",
                consumer_timeout_ms=-1,
            )
            print(f"[TMR-Voter] Inscrito nos tópicos: {TOPIC_MOTOR_A}, {TOPIC_MOTOR_B}, {TOPIC_MOTOR_C}", flush=True)
            break
        except Exception as e:
            print(f"[TMR-Voter] Kafka indisponível ({e}). Retry em 5s...", flush=True)
            time.sleep(5)

    for msg in consumer:
        topic = msg.topic
        payload = msg.value

        motor_id = None
        if topic == TOPIC_MOTOR_A:
            motor_id = "A"
        elif topic == TOPIC_MOTOR_B:
            motor_id = "B"
        elif topic == TOPIC_MOTOR_C:
            motor_id = "C"

        if motor_id:
            with lock:
                leituras[motor_id] = payload


# ── Thread: Ciclo de Votação ──────────────────────────────────

def voting_loop():
    """Executa votação TMR a cada INTERVALO_VOTACAO segundos."""
    print("[TMR-Voter] Aguardando Kafka producer...", flush=True)

    while True:
        try:
            producer = KafkaProducer(
                bootstrap_servers=KAFKA_BROKER,
                value_serializer=lambda v: json.dumps(v).encode("utf-8"),
            )
            print("[TMR-Voter] Producer Kafka pronto.", flush=True)
            break
        except Exception as e:
            print(f"[TMR-Voter] Kafka producer indisponível ({e}). Retry em 5s...", flush=True)
            time.sleep(5)

    while True:
        time.sleep(INTERVALO_VOTACAO)

        with lock:
            snapshot = {k: v for k, v in leituras.items()}

        # Precisa de pelo menos 2 motores para votar
        disponíveis = {k: v for k, v in snapshot.items() if v is not None}
        if len(disponíveis) < 2:
            continue

        # Se faltam dados de um motor, preenche com None
        grandezas_a = extrair_grandezas(snapshot["A"]) if snapshot["A"] else None
        grandezas_b = extrair_grandezas(snapshot["B"]) if snapshot["B"] else None
        grandezas_c = extrair_grandezas(snapshot["C"]) if snapshot["C"] else None

        # Se apenas 2 motores disponíveis, faz média dos 2
        if sum(1 for g in [grandezas_a, grandezas_b, grandezas_c] if g is not None) == 2:
            disponiveis_list = [g for g in [grandezas_a, grandezas_b, grandezas_c] if g is not None]
            resultado = {}
            for key in ["rpm", "temperature", "pressure"]:
                resultado[key] = round((disponiveis_list[0][key] + disponiveis_list[1][key]) / 2.0, 2)

            payload_consolidado = {
                "source": "tmr-voter",
                "type": "MOTOR_CONSOLIDATED",
                "timestamp": datetime.now(timezone.utc).isoformat(),
                "dados": resultado,
                "motores_ativos": 2,
            }
            producer.send(TOPIC_CONSOLIDATED, payload_consolidado)
            producer.flush()
            print(f"[TMR-Voter] Consolidado (2 motores): RPM={resultado['rpm']}, Temp={resultado['temperature']}, Press={resultado['pressure']}", flush=True)
            continue

        # TMR completo com 3 motores
        resultado = {}
        alertas_totais = []

        for key in ["rpm", "temperature", "pressure"]:
            val_a = grandezas_a[key]
            val_b = grandezas_b[key]
            val_c = grandezas_c[key]

            consenso, alertas = votar_grandeza(key, val_a, val_b, val_c, producer)
            resultado[key] = consenso
            alertas_totais.extend(alertas)

        # Publicar telemetria consolidada
        payload_consolidado = {
            "source": "tmr-voter",
            "type": "MOTOR_CONSOLIDATED",
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "dados": resultado,
            "motores_ativos": 3,
        }
        producer.send(TOPIC_CONSOLIDATED, payload_consolidado)

        # Publicar alertas
        for severidade, descricao, motor_falho in alertas_totais:
            alerta = {
                "tipo": "FALHA_MOTOR" if motor_falho else "FALHA_BIZANTINA",
                "descricao": descricao,
                "severidade": severidade,
                "origem": f"tmr-voter",
                "timestamp": datetime.now(timezone.utc).isoformat(),
            }
            producer.send(TOPIC_EVENTS, alerta)
            print(f"[TMR-Voter] ⚠️ ALERTA [{severidade}]: {descricao}", flush=True)

        producer.flush()
        print(f"[TMR-Voter] ✅ Consolidado: RPM={resultado['rpm']}, Temp={resultado['temperature']}, Press={resultado['pressure']}", flush=True)

        # Heartbeat para o detector de falhas
        producer.send(TOPIC_HEALTH, {
            "modulo": "VoterTMR",
            "status": "active",
            "timestamp": time.time(),
        })
        producer.flush()


# ── Main ──────────────────────────────────────────────────────

def main():
    print("=" * 60, flush=True)
    print("  ⚖️  TMR Voter — Redundância Modular Tripla  ", flush=True)
    print("=" * 60, flush=True)

    t_consumer = threading.Thread(target=consumer_loop, daemon=True)
    t_voter = threading.Thread(target=voting_loop, daemon=True)

    t_consumer.start()
    t_voter.start()

    try:
        while True:
            time.sleep(60)
    except KeyboardInterrupt:
        print("[TMR-Voter] Encerrando...", flush=True)


if __name__ == "__main__":
    main()
