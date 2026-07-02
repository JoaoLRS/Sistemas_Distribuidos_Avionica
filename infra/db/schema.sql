-- ============================================================
-- SISTEMA AVIÔNICO DISTRIBUÍDO — GATEWAY AFDX/WAIC
-- Schema DDL — PostgreSQL 17
-- Criado para persistir os dados que chegam via MQTT
-- ============================================================

-- Extensão para geração de UUIDs seguros
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- 1. TELEMETRIA DE VOO
--    Fonte: sensores_voo.py
--    Tópico MQTT: avionica/sensores/voo
--    Campos: combustível, altitude, estabilizador, pressão, Mach
-- ============================================================
CREATE TABLE IF NOT EXISTS telemetria_voo (
    id                  UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    recebido_em         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    combustivel_pct     NUMERIC(5,2),        -- % de combustível restante (0.00 - 100.00)
    altitude_ft         INTEGER,             -- Altitude em pés acima do nível do mar
    estabilizador_graus NUMERIC(4,1),        -- Posição do estabilizador horizontal em graus
    pressao_cabine_psi  NUMERIC(5,2),        -- Pressão da cabine em PSI
    velocidade_mach     NUMERIC(5,3),        -- Velocidade em Mach (ex: 0.802)
    origem              VARCHAR(100)          -- Identificador do sensor de origem
);

-- ============================================================
-- 2. TELEMETRIA DE FREIOS
--    Fonte: sensor_freio.py
--    Tópico MQTT: avionica/sensores/freios
-- ============================================================
CREATE TABLE IF NOT EXISTS telemetria_freios (
    id          UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    recebido_em TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    pressao_psi NUMERIC(7,2),               -- Pressão hidráulica dos freios em PSI
    origem      VARCHAR(100)
);

-- ============================================================
-- 3. TELEMETRIA DE RADAR EXTERNO
--    Fonte: radar_externo.py
--    Tópico MQTT: avionica/radar
-- ============================================================
CREATE TABLE IF NOT EXISTS telemetria_radar (
    id              UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    recebido_em     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    vento_knots     NUMERIC(6,1),            -- Velocidade do vento em nós
    turbulencia     VARCHAR(50),              -- Nível de turbulência (NENHUMA, LEVE, MODERADA, SEVERA)
    radar_clima     VARCHAR(100),             -- Condição climática detectada (ex: LIMPO, CHUVA, TEMPESTADE)
    temp_externa_c  NUMERIC(5,1),             -- Temperatura externa em graus Celsius
    qnh_hpa         NUMERIC(6,1),             -- Pressão barométrica de referência (QNH) em hPa
    atc_msg         TEXT,                     -- Última mensagem ATC recebida
    origem          VARCHAR(100)
);

-- ============================================================
-- 4. TELEMETRIA WAIC (Wireless Avionics Intra-Communications)
--    Fonte: lider_waic.py (sensores sem fio nas asas/motores)
--    Tópico MQTT: avionica/sensores/waic
-- ============================================================
CREATE TABLE IF NOT EXISTS telemetria_waic (
    id              UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    recebido_em     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    pressao_motor   NUMERIC(7,2),            -- Pressão do motor em PSI
    temperatura_c   NUMERIC(6,1),            -- Temperatura do motor em graus Celsius
    origem          VARCHAR(100)
);

-- ============================================================
-- 5. DADOS DO COMPUTADOR DE NAVEGAÇÃO
--    Fonte: computador_navegacao.py
--    Tópico MQTT: avionica/navegacao
-- ============================================================
CREATE TABLE IF NOT EXISTS telemetria_navegacao (
    id                  UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    recebido_em         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    proa_graus          NUMERIC(5,1),        -- Proa magnética (heading) em graus (0-360)
    vs_fpm              INTEGER,              -- Velocidade vertical em pés por minuto (fpm)
    piloto_automatico   BOOLEAN,              -- TRUE = piloto automático ativo
    waypoint_ativo      VARCHAR(100),         -- Nome/código do waypoint atual
    eta_minutos         INTEGER,              -- Tempo estimado de chegada em minutos
    origem              VARCHAR(100)
);

-- ============================================================
-- 6. ROTAS DO FMS (Flight Management System)
--    Fonte: fms_distribuido.py (usa API Ninjas para coordenadas)
--    Tópico MQTT: avionica/fms/dados
-- ============================================================
CREATE TABLE IF NOT EXISTS rotas_fms (
    id              UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    registrado_em   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    callsign        VARCHAR(20),             -- Callsign da aeronave associada
    icao_origem     CHAR(4),                 -- Código ICAO do aeroporto de origem (ex: SBGR)
    icao_destino    CHAR(4),                 -- Código ICAO do aeroporto de destino (ex: EGLL)
    rota_texto      VARCHAR(200),            -- Rota por extenso (ex: "SBGR → EGLL")
    distancia_nm    NUMERIC(8,1),            -- Distância calculada em milhas náuticas
    eta_minutos     INTEGER,                 -- ETA calculado em minutos
    ativa           BOOLEAN DEFAULT TRUE      -- TRUE = rota atualmente em uso pelo FMS
);

-- ============================================================
-- 7. ALERTAS E FALHAS DO SISTEMA
--    Fonte: sistema_alertas.py e injetor_falhas.py
--    Tópico MQTT: avionica/comandos/falhas
-- ============================================================
CREATE TABLE IF NOT EXISTS alertas (
    id              UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    registrado_em   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    tipo            VARCHAR(100) NOT NULL,   -- Tipo: FALHA_SENSOR, OVERSPEED, PRESSAO_BAIXA, etc.
    descricao       TEXT,                    -- Descrição detalhada do alerta
    severidade      VARCHAR(20) NOT NULL DEFAULT 'INFO'
                    CHECK (severidade IN ('INFO', 'WARNING', 'CRITICAL')),
    origem          VARCHAR(100),            -- Subsistema que gerou o alerta
    resolvido       BOOLEAN NOT NULL DEFAULT FALSE,  -- FALSE = alerta ainda ativo
    resolvido_em    TIMESTAMPTZ              -- Timestamp de quando foi resolvido
);

-- ============================================================
-- 8. EVENTOS DO SISTEMA ANTI-ICE (Autônomo)
--    Fonte: computador_automacao.py
--    Tópico MQTT: avionica/sistemas/anti_ice
-- ============================================================
CREATE TABLE IF NOT EXISTS eventos_anti_ice (
    id              UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    registrado_em   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status          VARCHAR(50),             -- ATIVADO | DESATIVADO | STAND_BY
    mensagem        TEXT,                    -- Mensagem gerada pelo sistema autônomo
    temperatura_c   NUMERIC(5,1),            -- Temperatura que disparou o evento
    origem          VARCHAR(100)
);

-- ============================================================
-- 9. LOG HISTÓRICO DO BARRAMENTO MQTT
--    Equivale ao recentMessages em AircraftTelemetryService.java,
--    mas persistido no banco para análise histórica
-- ============================================================
CREATE TABLE IF NOT EXISTS mensagens_barramento (
    id              BIGSERIAL    PRIMARY KEY,
    recebido_em     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    topico          VARCHAR(200) NOT NULL,   -- Tópico MQTT completo (ex: avionica/sensores/voo)
    payload_json    JSONB,                   -- Payload completo como JSON estruturado
    tamanho_bytes   INTEGER                  -- Tamanho original do payload em bytes
);

-- ============================================================
-- ÍNDICES — Otimizam consultas frequentes
-- ============================================================

-- Índices de tempo (ordenação DESC para buscar o mais recente)
CREATE INDEX IF NOT EXISTS idx_telemetria_voo_tempo       ON telemetria_voo       (recebido_em DESC);
CREATE INDEX IF NOT EXISTS idx_telemetria_freios_tempo    ON telemetria_freios    (recebido_em DESC);
CREATE INDEX IF NOT EXISTS idx_telemetria_radar_tempo     ON telemetria_radar     (recebido_em DESC);
CREATE INDEX IF NOT EXISTS idx_telemetria_waic_tempo      ON telemetria_waic      (recebido_em DESC);
CREATE INDEX IF NOT EXISTS idx_telemetria_nav_tempo       ON telemetria_navegacao (recebido_em DESC);

-- Índices de alertas
CREATE INDEX IF NOT EXISTS idx_alertas_tempo              ON alertas              (registrado_em DESC);
CREATE INDEX IF NOT EXISTS idx_alertas_severidade         ON alertas              (severidade);
CREATE INDEX IF NOT EXISTS idx_alertas_nao_resolvidos     ON alertas              (resolvido) WHERE resolvido = FALSE;

-- Índices de barramento MQTT
CREATE INDEX IF NOT EXISTS idx_mensagens_topico           ON mensagens_barramento (topico);
CREATE INDEX IF NOT EXISTS idx_mensagens_tempo            ON mensagens_barramento (recebido_em DESC);
CREATE INDEX IF NOT EXISTS idx_mensagens_payload          ON mensagens_barramento USING GIN (payload_json);

-- Índice de rota ativa
CREATE INDEX IF NOT EXISTS idx_rotas_ativa                ON rotas_fms            (ativa) WHERE ativa = TRUE;

-- ============================================================
-- 10. AERONAVES (Torre de Comando)
-- ============================================================
CREATE TABLE IF NOT EXISTS aeronaves (
    callsign             VARCHAR(20)  PRIMARY KEY,            -- Prefixo/Callsign exclusivo (ex: PR-AAA)
    modelo               VARCHAR(100) NOT NULL,               -- Modelo da aeronave (ex: Boeing 737)
    capacidade_combustivel INTEGER,                           -- Capacidade em litros
    velocidade_cruzeiro   INTEGER,                           -- Velocidade de cruzeiro recomendada em nós
    status               VARCHAR(50)  NOT NULL DEFAULT 'No Patio', -- No Patio, Em Preparacao, Em Voo
    ultima_atualizacao   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_aeronaves_status           ON aeronaves (status);

-- ============================================================
-- COMENTÁRIOS DE DOCUMENTAÇÃO
-- ============================================================
COMMENT ON TABLE telemetria_voo        IS 'Dados dos sensores gerais de voo via MQTT (tópico: avionica/sensores/voo)';
COMMENT ON TABLE telemetria_freios     IS 'Dados do sensor de freios hidráulicos (tópico: avionica/sensores/freios)';
COMMENT ON TABLE telemetria_radar      IS 'Dados do radar externo e meteorologia (tópico: avionica/radar)';
COMMENT ON TABLE telemetria_waic       IS 'Sensores sem fio WAIC nos motores e asas (tópico: avionica/sensores/waic)';
COMMENT ON TABLE telemetria_navegacao  IS 'Dados do computador de navegação (tópico: avionica/navegacao)';
COMMENT ON TABLE rotas_fms             IS 'Rotas calculadas pelo Flight Management System distribuído';
COMMENT ON TABLE alertas               IS 'Alertas e falhas do sistema distribuído aviônico';
COMMENT ON TABLE eventos_anti_ice      IS 'Eventos do sistema autônomo anti-gelo (tópico: avionica/sistemas/anti_ice)';
COMMENT ON TABLE mensagens_barramento  IS 'Log histórico de todas as mensagens do barramento MQTT aviônico';
COMMENT ON TABLE aeronaves             IS 'Aeronaves cadastradas no ecossistema de simulação pela Torre de Comando';

-- ============================================================
-- 11. TELEMETRIA ORDENADA POR RELÓGIO LÓGICO DE LAMPORT
--     Responsável: Nickolas / Nickollas
--     Consumidor Kafka: avionica.telemetry.*
-- ============================================================
CREATE TABLE IF NOT EXISTS telemetria_ordenada (
    id              BIGSERIAL    PRIMARY KEY,
    recebido_em     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    topico_kafka    VARCHAR(200) NOT NULL,
    sensor_origem   VARCHAR(100),
    logical_clock   BIGINT       NOT NULL,
    payload_json    JSONB        NOT NULL,
    callsign        VARCHAR(20)
);

CREATE INDEX IF NOT EXISTS idx_telemetria_lamport ON telemetria_ordenada (logical_clock ASC);
CREATE INDEX IF NOT EXISTS idx_telemetria_ord_tempo ON telemetria_ordenada (recebido_em DESC);

COMMENT ON TABLE telemetria_ordenada IS
    'Telemetria persistida em ordem causal usando Relógio Lógico de Lamport (Nickolas / Nickollas)';

-- ============================================================
-- 12. STATUS DOS MÓDULOS (Detector de Falhas - Gabriela)
-- ============================================================
CREATE TABLE IF NOT EXISTS module_status (
    modulo             VARCHAR(100) PRIMARY KEY,
    status             VARCHAR(50)  NOT NULL,
    ultima_atualizacao TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

INSERT INTO module_status (modulo, status, ultima_atualizacao)
VALUES ('Computador_Primario', 'UP', NOW()),
       ('Computador_Secundario', 'UP', NOW())
ON CONFLICT (modulo) DO NOTHING;
