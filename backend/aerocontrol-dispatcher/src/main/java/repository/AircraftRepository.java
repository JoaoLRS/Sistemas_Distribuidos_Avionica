package avionica.torrecomando.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import avionica.torrecomando.model.Aircraft;

@Repository
public class AircraftRepository {

    private final JdbcTemplate jdbc;

    public AircraftRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ========================================================
    // RowMapper reutilizável — converte ResultSet → Aircraft
    // ========================================================
    private static final RowMapper<Aircraft> AIRCRAFT_MAPPER = (ResultSet rs, int rowNum) -> {
        Aircraft a = new Aircraft();
        a.setCallsign(rs.getString("callsign"));
        a.setModelo(rs.getString("modelo"));
        a.setCapacidadeCombustivel(rs.getObject("capacidade_combustivel", Integer.class));
        a.setVelocidadeCruzeiro(rs.getObject("velocidade_cruzeiro", Integer.class));
        a.setStatus(rs.getString("status"));

        java.sql.Timestamp ts = rs.getTimestamp("ultima_atualizacao");
        if (ts != null) {
            a.setUltimaAtualizacao(ts.toInstant());
        }
        return a;
    };

    // ========================================================
    // CRUD — Aeronaves
    // ========================================================

    /**
     * Busca uma aeronave pelo callsign.
     * @return Optional vazio se não encontrada.
     */
    public Optional<Aircraft> findByCallsign(String callsign) {
        List<Aircraft> results = jdbc.query(
                "SELECT * FROM aeronaves WHERE callsign = ?",
                AIRCRAFT_MAPPER,
                callsign
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    /**
     * Verifica se já existe uma aeronave com o callsign informado.
     */
    public boolean existsByCallsign(String callsign) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM aeronaves WHERE callsign = ?",
                Integer.class,
                callsign
        );
        return count != null && count > 0;
    }

    /**
     * Cadastra uma nova aeronave com status inicial "No Patio".
     */
    public void insert(String callsign, String modelo,
                       Integer capacidadeCombustivel, Integer velocidadeCruzeiro) {
        jdbc.update("""
                INSERT INTO aeronaves
                    (callsign, modelo, capacidade_combustivel, velocidade_cruzeiro, status, ultima_atualizacao)
                VALUES (?, ?, ?, ?, 'No Patio', NOW())
                """,
                callsign, modelo, capacidadeCombustivel, velocidadeCruzeiro
        );
    }

    /**
     * Lista todas as aeronaves cadastradas, ordenadas por callsign.
     */
    public List<Aircraft> findAll() {
        return jdbc.query(
                "SELECT * FROM aeronaves ORDER BY callsign",
                AIRCRAFT_MAPPER
        );
    }

    /**
     * Lista todas as aeronaves filtradas por status.
     * Ex: findByStatus("Em Voo"), findByStatus("No Patio")
     */
    public List<Aircraft> findByStatus(String status) {
        return jdbc.query(
                "SELECT * FROM aeronaves WHERE status = ? ORDER BY callsign",
                AIRCRAFT_MAPPER,
                status
        );
    }

    /**
     * Atualiza o status de uma aeronave e marca ultima_atualizacao = NOW().
     * @return número de linhas afetadas (0 = callsign não encontrado).
     */
    public int updateStatus(String callsign, String novoStatus) {
        return jdbc.update(
                "UPDATE aeronaves SET status = ?, ultima_atualizacao = NOW() WHERE callsign = ?",
                novoStatus, callsign
        );
    }

    /**
     * Remove uma aeronave pelo callsign.
     * @return número de linhas afetadas (0 = callsign não encontrado).
     */
    public int deleteByCallsign(String callsign) {
        return jdbc.update(
                "DELETE FROM aeronaves WHERE callsign = ?",
                callsign
        );
    }

    /**
     * Conta o total de aeronaves cadastradas.
     */
    public int countAll() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM aeronaves", Integer.class);
        return count != null ? count : 0;
    }

    /**
     * Conta aeronaves por status.
     */
    public int countByStatus(String status) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM aeronaves WHERE status = ?",
                Integer.class,
                status
        );
        return count != null ? count : 0;
    }

    // ========================================================
    // LEITURA — Telemetria (somente consulta, sem escrita)
    // ========================================================

    /**
     * Busca a telemetria de voo mais recente (último registro).
     * Tabela: telemetria_voo
     */
    public Optional<Map<String, Object>> findLatestTelemetriaVoo() {
        List<Map<String, Object>> results = jdbc.queryForList(
                "SELECT * FROM telemetria_voo ORDER BY recebido_em DESC LIMIT 1"
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    /**
     * Busca as N telemetrias de voo mais recentes.
     */
    public List<Map<String, Object>> findRecentTelemetriaVoo(int limit) {
        return jdbc.queryForList(
                "SELECT * FROM telemetria_voo ORDER BY recebido_em DESC LIMIT ?",
                limit
        );
    }

    /**
     * Busca a última telemetria de radar externo.
     * Tabela: telemetria_radar
     */
    public Optional<Map<String, Object>> findLatestTelemetriaRadar() {
        List<Map<String, Object>> results = jdbc.queryForList(
                "SELECT * FROM telemetria_radar ORDER BY recebido_em DESC LIMIT 1"
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    /**
     * Busca a última telemetria de navegação.
     * Tabela: telemetria_navegacao
     */
    public Optional<Map<String, Object>> findLatestTelemetriaNavegacao() {
        List<Map<String, Object>> results = jdbc.queryForList(
                "SELECT * FROM telemetria_navegacao ORDER BY recebido_em DESC LIMIT 1"
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    /**
     * Busca a última telemetria de freios.
     * Tabela: telemetria_freios
     */
    public Optional<Map<String, Object>> findLatestTelemetriaFreios() {
        List<Map<String, Object>> results = jdbc.queryForList(
                "SELECT * FROM telemetria_freios ORDER BY recebido_em DESC LIMIT 1"
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    /**
     * Busca a última telemetria WAIC (sensores wireless dos motores/asas).
     * Tabela: telemetria_waic
     */
    public Optional<Map<String, Object>> findLatestTelemetriaWaic() {
        List<Map<String, Object>> results = jdbc.queryForList(
                "SELECT * FROM telemetria_waic ORDER BY recebido_em DESC LIMIT 1"
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    /**
     * Busca alertas não resolvidos, ordenados por severidade e data.
     * Tabela: alertas
     */
    public List<Map<String, Object>> findAlertasAtivos() {
        return jdbc.queryForList("""
                SELECT * FROM alertas
                WHERE resolvido = FALSE
                ORDER BY
                    CASE severidade
                        WHEN 'CRITICAL' THEN 1
                        WHEN 'WARNING'  THEN 2
                        WHEN 'INFO'     THEN 3
                    END,
                    registrado_em DESC
                """);
    }

    /**
     * Busca a rota FMS ativa.
     * Tabela: rotas_fms
     */
    public Optional<Map<String, Object>> findRotaAtiva() {
        List<Map<String, Object>> results = jdbc.queryForList(
                "SELECT * FROM rotas_fms WHERE ativa = TRUE ORDER BY registrado_em DESC LIMIT 1"
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    /**
     * Snapshot consolidado: busca o último dado de cada tipo de telemetria
     * para montar a visão da Torre de Comando.
     */
    public Map<String, Object> findTelemetrySnapshot() {
        return Map.of(
                "voo",       findLatestTelemetriaVoo().orElse(Map.of()),
                "radar",     findLatestTelemetriaRadar().orElse(Map.of()),
                "navegacao",  findLatestTelemetriaNavegacao().orElse(Map.of()),
                "freios",    findLatestTelemetriaFreios().orElse(Map.of()),
                "waic",      findLatestTelemetriaWaic().orElse(Map.of()),
                "rota_ativa", findRotaAtiva().orElse(Map.of()),
                "alertas",   findAlertasAtivos()
        );
    }
}