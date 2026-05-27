import axios from 'axios'

const http = axios.create({
  baseURL: '/api/v1',
  timeout: 10000,
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
    return Promise.reject(err)
  },
)

export async function listLikes(userId) {
  const { data } = await http.get('/likes', { params: { userId } })
  return data.data ?? []
}

export async function createLike(payload) {
  const { data } = await http.post('/likes', payload)
  return data.data
}

export async function updateLike(sn, payload) {
  await http.put(`/likes/${sn}`, payload)
}

export async function deleteLike(sn) {
  await http.delete(`/likes/${sn}`)
}

export const SEED_USERS = [
  { id: 'A1236456789', name: '王o明' },
  { id: 'B9876543210', name: '陳o華' },
]
