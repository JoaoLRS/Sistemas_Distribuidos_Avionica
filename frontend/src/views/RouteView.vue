<template>
  <div class="container px-4 py-5">
    <div class="panel mx-auto p-4" style="max-width: 600px;">
      <h2 class="h4 mb-4 text-white text-center">Definir Rota de Voo</h2>
      <h3 class="h6 text-info text-center mb-4 mono">Aeronave Selecionada: {{ callsign }}</h3>
      
      <form @submit.prevent="startSimulation">
        <div class="mb-3">
          <label class="form-label text-secondary small">Aeroporto de Origem (ICAO)</label>
          <input 
            v-model="origin" 
            type="text" 
            class="form-control" 
            placeholder="Ex: SBGR" 
            maxlength="4" 
            required
          />
        </div>

        <div class="mb-4">
          <label class="form-label text-secondary small">Aeroporto de Destino (ICAO)</label>
          <input 
            v-model="destination" 
            type="text" 
            class="form-control" 
            placeholder="Ex: SBRJ" 
            maxlength="4" 
            required
          />
        </div>

        <div v-if="error" class="alert alert-danger text-xs py-2 px-3 mb-3">
          {{ error }}
        </div>

        <div class="d-flex gap-3">
          <button 
            type="button" 
            @click="goBack" 
            class="btn btn-outline-light w-50"
          >
            Voltar
          </button>
          <button 
            type="submit" 
            class="btn btn-primary w-50" 
            :disabled="loading || !origin || !destination"
          >
            {{ loading ? 'Consensualizando...' : 'Simular' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import axios from 'axios';

const route = useRoute();
const router = useRouter();

const apiBaseUrl = 'http://localhost:8080';
const dispatcherUrl = 'http://localhost:8082';

const callsign = ref('');
const origin = ref('');
const destination = ref('');
const loading = ref(false);
const error = ref('');

function goBack() {
  router.push('/simulacao');
}

async function startSimulation() {
  if (!callsign.value || !origin.value || !destination.value) return;

  loading.value = true;
  error.value = '';

  const cleanOrigin = origin.value.toUpperCase().trim();
  const cleanDestination = destination.value.toUpperCase().trim();

  try {
    // 1️⃣ Primeiro calcula a rota no FMS (precisa existir no banco antes do consenso)
    await axios.post(`${apiBaseUrl}/api/routes`, {
      callsign: callsign.value,
      origin: cleanOrigin,
      destination: cleanDestination
    });

    // 2️⃣ Agora solicita o consenso de decolagem (valida clima, computadores e rota FMS)
    const response = await axios.post(`${dispatcherUrl}/api/dispatcher/aircraft/${callsign.value}/takeoff`);

    if (response.data && response.data.message) {
      alert(response.data.message); // Exibe feedback do consenso
    }

    // Redireciona para o dashboard seguindo a convenção do CDU-04
    router.push(`/simulacao/dashboard/${callsign.value}/${cleanOrigin}/${cleanDestination}`);
  } catch (err) {
    error.value = err.response?.data?.erro || err.response?.data?.error || 'Decolagem Recusada pela Torre de Comando (AeroControl).';
    console.error(err);
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  callsign.value = route.query.callsign;
  if (!callsign.value) {
    router.push('/simulacao');
  }
});
</script>
