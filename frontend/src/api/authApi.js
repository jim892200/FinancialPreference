import { http } from './http.js'

export async function login(userId, password) {
  const { data } = await http.post('/auth/login', { userId, password })
  return data.data
}
