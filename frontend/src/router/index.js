import { createRouter, createWebHistory } from 'vue-router';
import HomeView from '../views/HomeView.vue';
import SimulationView from '../views/SimulationView.vue';
import RouteView from '../views/RouteView.vue';
import DashboardView from '../views/DashboardView.vue';

const routes = [
  { path: '/', redirect: '/painel' },
  { path: '/painel', name: 'painel', component: HomeView },
  { path: '/simulacao', name: 'simulacao', component: SimulationView },
  { path: '/simulacao/rota', name: 'simulacao-rota', component: RouteView },
  { path: '/simulacao/dashboard/:id_aeronave_:id_decolagem_:id_destino', name: 'simulacao-dashboard', component: DashboardView, props: true }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

export default router;
