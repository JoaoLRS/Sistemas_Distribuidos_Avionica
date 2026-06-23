package avionica.audit.dto;

public class SensorAuditDto {
    private String sensor;
    private long totalMensagens;
    private long ultimoClock;
    private long ultimoTime;
    private long anomaliasCausais;
    private long mensagensPerdidas;

    public SensorAuditDto(String sensor, long clock, long time) {
        this.sensor = sensor;
        this.totalMensagens = 1;
        this.ultimoClock = clock;
        this.ultimoTime = time;
        this.anomaliasCausais = 0;
        this.mensagensPerdidas = 0;
    }

    public String getSensor() { return sensor; }
    public long getTotalMensagens() { return totalMensagens; }
    public long getUltimoClock() { return ultimoClock; }
    public long getUltimoTime() { return ultimoTime; }
    public long getAnomaliasCausais() { return anomaliasCausais; }
    public long getMensagensPerdidas() { return mensagensPerdidas; }

    public void incrementMensagens() { this.totalMensagens++; }
    public void setUltimoClock(long clock) { this.ultimoClock = clock; }
    public void setUltimoTime(long time) { this.ultimoTime = time; }
    public void incrementAnomalias() { this.anomaliasCausais++; }
    public void addMensagensPerdidas(long count) { this.mensagensPerdidas += count; }
}
