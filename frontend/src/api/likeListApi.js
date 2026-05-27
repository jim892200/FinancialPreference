import { http } from './http.js'

export async function listLikes() {
  const { data } = await http.get('/likes')
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
