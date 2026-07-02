<template>
  <div class="container-fluid px-4 py-4">
    <!-- Cabeçalho do Voo -->
    <div class="panel d-flex justify-content-between align-items-center mb-4 py-3">
      <div>
        <h2 class="h5 text-white mb-1 mono">SGCA COCKPIT — FLIGHT OVERVIEW</h2>
        <div class="text-secondary small mono">
          Aeronave: <span class="text-info fw-bold">{{ id_aeronave }}</span> | 
          Rota: <span class="text-info">{{ id_decolagem }} ➔ {{ id_destino }}</span>
        </div>
      </div>
      <div class="d-flex align-items-center gap-3">
        <span class="badge status-badge green" v-if="telemetry.flight?.altitude_ft > 0">
          EM VOO
        </span>
        <span class="badge status-badge amber" v-else>
          PREPARAÇÃO
        </span>
        <button @click="stopSimulation" class="btn btn-danger btn-sm">
          ⚠️ Encerrar Simulação
        </button>
      </div>
    </div>

    <!-- Layout Principal do Glass Cockpit -->
    <div class="row g-4">
      
      <!-- Coluna Esquerda: Painel de Atitude e Fitas (CDU-04 Principal) -->
      <div class="col-12 col-xl-8">
        <div class="panel p-4 d-flex flex-column align-items-center bg-dark/20 border-secondary">
          <h3 class="h6 text-secondary text-uppercase fw-semibold mb-4 w-100 text-center mono">
            Primary Flight Display (PFD)
          </h3>
          
          <div class="pfd-layout w-100 d-flex justify-content-center align-items-center gap-3">
            
            <!-- FITA DE VELOCIDADE (LEFT TAPE) -->
            <div class="tape-container speed-tape">
              <div class="tape-label">SPD</div>
              <div class="tape-window text-info mono fw-bold text-center">
                {{ formatSpeed(telemetry.flight?.velocidade_mach) }}
              </div>
              <div class="tape-ruler">
                <!-- Fita móvel baseada na velocidade -->
                <div class="tape-ticks" :style="{ transform: `translateY(${speedOffset}px)` }">
                  <div v-for="spd in speedTicks" :key="spd" class="tick-mark">
                    <span class="tick-num">{{ spd }}</span>
                    <span class="tick-line"></span>
                  </div>
                </div>
              </div>
            </div>

            <!-- HORIZONTE ARTIFICIAL (CENTER) -->
            <div class="horizon-wrapper">
              <svg class="horizon-svg" viewBox="0 0 300 300">
                <defs>
                  <clipPath id="horizon-clip">
                    <circle cx="150" cy="150" r="140" />
                  </clipPath>
                </defs>
                <!-- Grupo móvel de atitude (Pitch e Roll) -->
                <g :transform="`rotate(${roll}) translate(0, ${pitchOffset})`" clip-path="url(#horizon-clip)">
                  <!-- Céu -->
                  <rect x="-100" y="-300" width="500" height="450" fill="#00b8ff" />
                  <!-- Terra -->
                  <rect x="-100" y="150" width="500" height="450" fill="#5c4033" />
                  <!-- Linha do Horizonte -->
                  <line x1="-100" y1="150" x2="400" y2="150" stroke="#fff" stroke-width="3" />
                  <!-- Escada de Pitch -->
                  <g stroke="#fff" stroke-width="1.5" opacity="0.8">
                    <!-- +10 Deg -->
                    <line x1="120" y1="100" x2="180" y2="100" />
                    <text x="190" y="105" fill="#fff" font-size="10" font-family="monospace">10</text>
                    <!-- +20 Deg -->
                    <line x1="130" y1="50" x2="170" y2="50" />
                    <text x="180" y="55" fill="#fff" font-size="10" font-family="monospace">20</text>
                    <!-- -10 Deg -->
                    <line x1="120" y1="200" x2="180" y2="200" />
                    <text x="190" y="205" fill="#fff" font-size="10" font-family="monospace">-10</text>
                    <!-- -20 Deg -->
                    <line x1="130" y1="250" x2="170" y2="250" />
                    <text x="180" y="255" fill="#fff" font-size="10" font-family="monospace">-20</text>
                  </g>
                </g>

                <!-- Aro de Escala de Rolagem Fixo -->
                <circle cx="150" cy="150" r="140" fill="none" stroke="rgba(255,255,255,0.15)" stroke-width="2" />
                <g fill="#fff" font-size="10" font-family="monospace" text-anchor="middle">
                  <text x="150" y="25">▼</text>
                </g>

                <!-- Símbolo Estático do Avião (Amarelo) -->
                <g stroke="#ffc107" stroke-width="4" fill="none" stroke-linecap="round">
                  <!-- Asa Esquerda -->
                  <line x1="80" y1="150" x2="120" y2="150" />
                  <!-- Asa Direita -->
                  <line x1="180" y1="150" x2="220" y2="150" />
                  <!-- Centro -->
                  <circle cx="150" cy="150" r="4" fill="#ffc107" />
                  <line x1="140" y1="160" x2="160" y2="160" />
                </g>
              </svg>
              
              <!-- Indicador de Proa (Heading) -->
              <div class="heading-indicator text-center mono mt-2">
                HDG <span class="text-white fw-bold">{{ formatHeading(telemetry.navigation?.proa_graus) }}°</span>
              </div>
            </div>

            <!-- FITA DE ALTITUDE (RIGHT TAPE) -->
            <div class="tape-container altitude-tape">
              <div class="tape-label">ALT</div>
              <div class="tape-window text-info mono fw-bold text-center">
                {{ formatAltitude(telemetry.flight?.altitude_ft) }}
              </div>
              <div class="tape-ruler">
                <!-- Fita móvel baseada na altitude -->
                <div class="tape-ticks" :style="{ transform: `translateY(${altitudeOffset}px)` }">
                  <div v-for="alt in altitudeTicks" :key="alt" class="tick-mark">
                    <span class="tick-num">{{ alt }}</span>
                    <span class="tick-line"></span>
                  </div>
                </div>
              </div>
            </div>

          </div>
        </div>
      </div>

      <!-- Coluna Direita: Painéis Auxiliares (FMS, Combustível, Freios, CAS, Clima) -->
      <div class="col-12 col-xl-4 d-flex flex-column gap-4">
        
        <!-- Painel de Rota e ETA -->
        <div class="panel p-3">
          <h3 class="h6 text-secondary text-uppercase fw-semibold mb-3 mono">📍 Rota e Planejamento</h3>
          <div class="row text-xs mono">
            <div class="col-6 mb-2">
              <div class="text-secondary">Origem</div>
              <div class="text-white fw-bold fs-6">{{ telemetry.fms?.origem || id_decolagem }}</div>
            </div>
            <div class="col-6 mb-2">
              <div class="text-secondary">Destino</div>
              <div class="text-white fw-bold fs-6">{{ telemetry.fms?.destino || id_destino }}</div>
            </div>
            <div class="col-6">
              <div class="text-secondary">Distância Restante</div>
              <div class="text-white fw-bold">{{ telemetry.fms?.distancia_nm || 0 }} NM</div>
            </div>
            <div class="col-6">
              <div class="text-secondary">ETA Estimado</div>
              <div class="text-white fw-bold">{{ telemetry.fms?.eta_minutos || 0 }} min</div>
            </div>
          </div>
        </div>

        <!-- Combustível e Freios -->
        <div class="panel p-3">
          <h3 class="h6 text-secondary text-uppercase fw-semibold mb-3 mono">⚙️ Sistemas do Motor e Hidráulica</h3>
          <div class="mb-3">
            <div class="d-flex justify-content-between text-xs mb-1">
              <span class="text-secondary">Status de Combustível</span>
              <span class="text-white fw-semibold">{{ telemetry.flight?.combustivel_pct || 100 }}%</span>
            </div>
            <div class="progress bg-dark" style="height: 6px;">
              <div class="progress-bar bg-success" :style="{ width: `${telemetry.flight?.combustivel_pct || 100}%` }"></div>
            </div>
          </div>
          <div>
            <div class="d-flex justify-content-between text-xs mb-1">
              <span class="text-secondary">Pressão dos Freios</span>
              <span class="text-white fw-semibold">{{ telemetry.brakes?.pressao || 3000 }} psi</span>
            </div>
            <div class="progress bg-dark" style="height: 6px;">
              <div class="progress-bar bg-info" :style="{ width: `${Math.min(((telemetry.brakes?.pressao || 3000) / 3500) * 100, 100)}%` }"></div>
            </div>
          </div>
        </div>

        <!-- Radar Climático -->
        <div class="panel p-3">
          <h3 class="h6 text-secondary text-uppercase fw-semibold mb-3 mono">⛈️ Radar Climático</h3>
          <div class="row text-xs mono">
            <div class="col-6 mb-2">
              <div class="text-secondary">Condição</div>
              <div class="fw-bold" :class="getWeatherClass(telemetry.radar?.radar_clima)">
                {{ telemetry.radar?.radar_clima || 'LIMPO' }}
              </div>
            </div>
            <div class="col-6 mb-2">
              <div class="text-secondary">Vento Externo</div>
              <div class="text-white fw-bold">{{ telemetry.radar?.vento_knots || 0 }} kt</div>
            </div>
            <div class="col-6">
              <div class="text-secondary">Temp. Externa</div>
              <div class="text-white fw-bold">{{ telemetry.radar?.temp_externa_c || 15 }} °C</div>
            </div>
            <div class="col-6">
              <div class="text-secondary">Turbulência</div>
              <div class="fw-bold" :class="getTurbulenceClass(telemetry.radar?.turbulencia)">
                {{ telemetry.radar?.turbulencia || 'NENHUMA' }}
              </div>
            </div>
          </div>
        </div>

        <!-- Crew Alerting System (CAS) -->
        <div class="panel p-3 d-flex flex-column" style="flex: 1; min-height: 180px;">
          <h3 class="h6 text-secondary text-uppercase fw-semibold mb-2 mono">🚨 Crew Alerting System (CAS)</h3>
          <div class="cas-box overflow-y-auto flex-grow-1" style="max-height: 180px;">
            <div v-if="activeAlerts.length === 0" class="text-success small mono py-2 text-center">
              ✔ ALL SYSTEMS NOMINAL
            </div>
            <div v-else>
              <div v-for="alert in activeAlerts" :key="alert.id" class="alert-item py-1 border-bottom border-secondary/30 mono text-xs d-flex justify-content-between align-items-center">
                <span :class="alert.severidade === 'CRITICAL' ? 'text-danger fw-bold' : 'text-warning'">
                  ⚠ {{ alert.descricao }}
                </span>
                <span class="badge" :class="alert.severidade === 'CRITICAL' ? 'bg-danger' : 'bg-warning text-dark'">
                  {{ alert.severidade }}
                </span>
              </div>
            </div>
          </div>
        </div>

      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';

const props = defineProps({
  id_aeronave: { type: String, required: true },
  id_decolagem: { type: String, required: true },
  id_destino: { type: String, required: true }
});

const router = useRouter();
const apiBaseUrl = 'http://localhost:8080';
const dispatcherUrl = 'http://localhost:8082';

const telemetry = ref({});
const activeAlerts = ref([]);
let pollTimer = null;

// Mock Pitch e Roll baseados em VS e Turbulência (CDU-04 Requisito)
const pitch = computed(() => {
  const vs = telemetry.value.navigation?.vs_fpm || 0;
  // Mapeia -1500 fpm a +1500 fpm para -15 a +15 graus de pitch
  const calculatedPitch = Math.max(-25, Math.min(25, vs / 80));
  return calculatedPitch;
});

const roll = computed(() => {
  // Pequena oscilação para simular o comportamento da turbulência no cockpit
  const turbulence = telemetry.value.radar?.turbulencia || 'NENHUMA';
  let factor = 1.0;
  if (turbulence === 'MODERADA') factor = 4.0;
  else if (turbulence === 'SEVERA') factor = 8.0;

  const seconds = new Date().getTime() / 1000;
  return Math.sin(seconds * 2) * factor;
});

// Offsets para as fitas PFD
const pitchOffset = computed(() => {
  // Ajuste de pixel da translação do Horizonte baseada no Pitch
  return pitch.value * 3;
});

const speedOffset = computed(() => {
  const speed = formatSpeedValue(telemetry.value.flight?.velocidade_mach);
  return (speed % 50) * 1.5;
});

const altitudeOffset = computed(() => {
  const alt = telemetry.value.flight?.altitude_ft || 0;
  return (alt % 1000) * 0.08;
});

// Ticks para as fitas
const speedTicks = computed(() => {
  const speed = formatSpeedValue(telemetry.value.flight?.velocidade_mach);
  const base = Math.floor(speed / 50) * 50;
  return [base + 100, base + 50, base, base - 50, base - 100].filter(s => s >= 0);
});

const altitudeTicks = computed(() => {
  const alt = telemetry.value.flight?.altitude_ft || 0;
  const base = Math.floor(alt / 1000) * 1000;
  return [base + 2000, base + 1000, base, base - 1000, base - 2000].filter(a => a >= 0);
});

function formatSpeedValue(mach) {
  if (!mach) return 0;
  // Converter Mach em nós estimado para a fita (Ex: Mach 0.8 => 530 kt)
  return Math.round(mach * 661);
}

function formatSpeed(mach) {
  if (!mach) return '0 KT';
  return `${formatSpeedValue(mach)} KT / M ${mach}`;
}

function formatAltitude(alt) {
  if (!alt) return '0 FT';
  return `${Math.round(alt)} FT`;
}

function formatHeading(heading) {
  if (!heading) return '360';
  return String(Math.round(heading)).padStart(3, '0');
}

function getWeatherClass(clima) {
  if (clima === 'TEMPESTADE') return 'text-danger';
  if (clima === 'NUBLADO') return 'text-warning';
  return 'text-success';
}

function getTurbulenceClass(turb) {
  if (turb === 'SEVERA') return 'text-danger fw-bold';
  if (turb === 'MODERADA') return 'text-warning';
  return 'text-secondary';
}

// Buscar Dados
async function fetchTelemetry() {
  try {
    const res = await axios.get(`${apiBaseUrl}/api/aircraft-data`);
    telemetry.value = res.data;
  } catch (err) {
    console.error('Erro de polling de telemetria:', err);
  }
}

async function fetchAlerts() {
  try {
    // Buscar alertas ativos da aeronave na torre
    const res = await axios.get(`${dispatcherUrl}/api/dispatcher/aircraft/telemetry/snapshot`);
    if (res.data && res.data.alertas) {
      activeAlerts.value = res.data.alertas;
    } else {
      activeAlerts.value = [];
    }
  } catch (err) {
    console.error('Erro ao carregar alertas do CAS:', err);
  }
}

async function stopSimulation() {
  const confirmEnd = confirm('Tem certeza que deseja encerrar o voo e finalizar a simulação?');
  if (!confirmEnd) return;

  try {
    await axios.post(`${apiBaseUrl}/api/routes/stop`, {
      callsign: props.id_aeronave
    });
    router.push('/simulacao');
  } catch (err) {
    alert('Erro ao parar a simulação: ' + (err.response?.data?.erro || err.message));
  }
}

onMounted(() => {
  fetchTelemetry();
  fetchAlerts();
  pollTimer = setInterval(() => {
    fetchTelemetry();
    fetchAlerts();
  }, 1000);
});

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer);
});
</script>

<style scoped>
.pfd-layout {
  height: 380px;
}

.horizon-wrapper {
  position: relative;
  width: 300px;
  height: 300px;
  background: #000;
  border-radius: 50%;
  border: 4px stroke var(--border-glass);
  overflow: hidden;
  box-shadow: inset 0 0 20px rgba(0,0,0,0.8);
}

.horizon-svg {
  width: 100%;
  height: 100%;
}

.tape-container {
  display: flex;
  flex-direction: column;
  width: 80px;
  height: 300px;
  background: rgba(10, 12, 18, 0.7);
  border: 1px solid var(--border-glass);
  border-radius: 8px;
  overflow: hidden;
  position: relative;
}

.tape-label {
  background: rgba(255,255,255,0.05);
  font-size: 0.65rem;
  font-weight: 700;
  text-align: center;
  padding: 2px 0;
  color: var(--text-secondary);
  border-bottom: 1px solid var(--border-glass);
}

.tape-window {
  background: #07090e;
  border-bottom: 1px solid var(--border-glass);
  font-size: 0.75rem;
  padding: 6px 2px;
}

.tape-ruler {
  flex-grow: 1;
  position: relative;
  overflow: hidden;
}

.tape-ticks {
  position: absolute;
  left: 0;
  right: 0;
  top: 110px; /* Offset central */
  transition: transform 0.2s linear;
}

.tick-mark {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 40px; /* Distância uniforme de escala */
  padding: 0 10px;
  border-bottom: 1px solid rgba(255,255,255,0.03);
}

.tick-num {
  font-family: monospace;
  font-size: 0.75rem;
  color: var(--text-secondary);
}

.tick-line {
  width: 10px;
  height: 2px;
  background: rgba(255,255,255,0.3);
}
</style>
