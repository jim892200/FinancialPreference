import { defineStore } from 'pinia'

const TOKEN_KEY = 'fp_token'
const USER_ID_KEY = 'fp_user_id'
const USER_NAME_KEY = 'fp_user_name'
const ACCOUNT_KEY = 'fp_account'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    userId: localStorage.getItem(USER_ID_KEY) || '',
    userName: localStorage.getItem(USER_NAME_KEY) || '',
    account: localStorage.getItem(ACCOUNT_KEY) || '',
  }),
  getters: {
    isAuthenticated: (s) => !!s.token,
  },
  actions: {
    setSession({ token, userId, userName, account }) {
      this.token = token
      this.userId = userId
      this.userName = userName
      this.account = account || ''
      localStorage.setItem(TOKEN_KEY, token)
      localStorage.setItem(USER_ID_KEY, userId)
      localStorage.setItem(USER_NAME_KEY, userName)
      localStorage.setItem(ACCOUNT_KEY, this.account)
    },
    clear() {
      this.token = ''
      this.userId = ''
      this.userName = ''
      this.account = ''
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_ID_KEY)
      localStorage.removeItem(USER_NAME_KEY)
      localStorage.removeItem(ACCOUNT_KEY)
    },
  },
})
