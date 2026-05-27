import { http } from './http.js'

export async function listLikes(params = {}) {
  const cleaned = {}
  for (const [k, v] of Object.entries(params)) {
    if (v !== null && v !== undefined && v !== '') cleaned[k] = v
  }
  const { data } = await http.get('/likes', { params: cleaned })
  return data.data ?? { items: [], total: 0, page: 1, pageSize: 10 }
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
