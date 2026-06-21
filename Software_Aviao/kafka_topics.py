MQTT_TO_KAFKA = {
    "avionica/sensores/voo": {
        "topic": "avionica.telemetry.flight",
        "type": "TELEMETRY_FLIGHT"
    },
    "avionica/sensores/freios": {
        "topic": "avionica.telemetry.brakes",
        "type": "TELEMETRY_BRAKE"
    },
    "avionica/radar": {
        "topic": "avionica.telemetry.radar",
        "type": "TELEMETRY_RADAR"
    },
    "avionica/fms/dados": {
        "topic": "avionica.route.calculated",
        "type": "ROUTE_CALCULATED"
    },
    "avionica/navegacao": {
        "topic": "avionica.navigation",
        "type": "NAVIGATION"
    },
    "avionica/sensores/waic": {
        "topic": "avionica.telemetry.waic",
        "type": "TELEMETRY_WAIC"
    },
    "avionica/sistemas/anti_ice": {
        "topic": "avionica.automation.anti_ice",
        "type": "ANTI_ICE_EVENT"
    },
    "avionica/comandos/falhas": {
        "topic": "avionica.system.events",
        "type": "SYSTEM_EVENT"
    }
}

RICART_TOPICS = [
    "avionica.mutex.brakes.request",
    "avionica.mutex.brakes.grant",
    "avionica.mutex.brakes.release",
]

KAFKA_TOPICS = sorted(set([config["topic"] for config in MQTT_TO_KAFKA.values()] + RICART_TOPICS))
