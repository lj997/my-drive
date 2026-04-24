import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login, getUserInfo } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(null)

  const setToken = (newToken) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  const setUserInfo = (info) => {
    userInfo.value = info
  }

  const loginAction = async (loginForm) => {
    const res = await login(loginForm)
    setToken(res.data.token)
    setUserInfo(res.data.user)
    return res
  }

  const fetchUserInfo = async () => {
    const res = await getUserInfo()
    setUserInfo(res.data)
    return res
  }

  const logout = () => {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
  }

  return {
    token,
    userInfo,
    setToken,
    setUserInfo,
    loginAction,
    fetchUserInfo,
    logout
  }
})
