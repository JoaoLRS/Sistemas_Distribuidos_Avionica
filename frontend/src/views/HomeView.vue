<template>
  <div class="container px-4 py-5 text-center">
    <div class="panel mx-auto py-5" style="max-width: 800px;">
      <span class="fs-1">✈️</span>
      <h1 class="display-5 fw-bold text-white mb-4">SGCA — Simulador Glass Cockpit da Aeronave</h1>
      <p class="lead text-secondary mb-5">
        Bem-vindo ao SGCA. Este sistema simula em tempo real os instrumentos aviônicos distribuídos de uma aeronave comercial, monitorando motores, telemetria de voo, sistemas de freios e radar meteorológico integrados a um barramento de eventos distribuído e tolerante a falhas.
      </p>
      <div class="d-grid gap-2 d-sm-flex justify-content-sm-center">
        <router-link to="/simulacao" class="btn btn-primary btn-lg px-5 gap-3">
          Iniciar Simulação
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
</script>
