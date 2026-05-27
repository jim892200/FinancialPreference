import { createRouter, createWebHistory } from 'vue-router'
import LikeListView from '../views/LikeListView.vue'
import LikeFormView from '../views/LikeFormView.vue'
import LoginView from '../views/LoginView.vue'
import { useAuthStore } from '../stores/auth.js'

const routes = [
  { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
  { path: '/', name: 'list', component: LikeListView },
  { path: '/new', name: 'create', component: LikeFormView, props: { mode: 'create' } },
  {
    path: '/edit/:sn',
    name: 'edit',
    component: LikeFormView,
    props: (r) => ({ mode: 'edit', sn: Number(r.params.sn) }),
  },
]

export const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  if (to.meta.public) return true
  const auth = useAuthStore()
  if (!auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})
