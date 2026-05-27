import { createRouter, createWebHistory } from 'vue-router'
import LikeListView from '../views/LikeListView.vue'
import LikeFormView from '../views/LikeFormView.vue'

const routes = [
  { path: '/', name: 'list', component: LikeListView },
  { path: '/new', name: 'create', component: LikeFormView, props: { mode: 'create' } },
  { path: '/edit/:sn', name: 'edit', component: LikeFormView, props: (r) => ({ mode: 'edit', sn: Number(r.params.sn) }) },
]

export const router = createRouter({
  history: createWebHistory(),
  routes,
})
