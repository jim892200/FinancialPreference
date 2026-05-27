import axios from 'axios'
import { useAuthStore } from '../stores/auth.js'
import { router } from '../router/index.js'

export const http = axios.create({
  baseURL: '/api/v1',
  timeout: 10000,
})

http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

http.interceptors.response.use(
  (resp) => resp,
  (err) => {
    const body = err?.response?.data
    if (body && typeof body === 'object' && body.code && body.message) {
      err.apiMessage = `${body.code} - ${body.message}`
    } else {
      err.apiMessage = err.message
    }

    if (err?.response?.status === 401) {
      const auth = useAuthStore()
      auth.clear()
      if (router.currentRoute.value.name !== 'login') {
        router.replace({
          name: 'login',
          query: { redirect: router.currentRoute.value.fullPath },
        })
      }
    }

    return Promise.reject(err)
  },
)
