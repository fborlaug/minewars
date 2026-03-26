import { defineStore } from 'pinia'
import { ref, computed, readonly } from 'vue'

interface JwtPayload {
  exp?: number
  [key: string]: unknown
}

function parsePayload(token: string): JwtPayload | null {
  try {
    const part = token.split('.')[1]
    if (!part) return null
    return JSON.parse(atob(part)) as JwtPayload
  } catch {
    return null
  }
}

function isTokenValid(token: string | null): boolean {
  if (!token) return false
  const payload = parsePayload(token)
  if (!payload) return false
  return !payload.exp || payload.exp * 1000 > Date.now()
}

function msUntilExpiry(token: string): number | null {
  const payload = parsePayload(token)
  if (!payload?.exp) return null
  return payload.exp * 1000 - Date.now()
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const username = ref<string | null>(localStorage.getItem('username'))
  let expiryTimer: ReturnType<typeof setTimeout> | null = null
  let onExpiry: (() => void) | null = null

  const isLoggedIn = computed(() => isTokenValid(token.value))

  /** Register a callback invoked after auto-logout (e.g. redirect to login). */
  function onTokenExpired(cb: () => void) {
    onExpiry = cb
  }

  function scheduleLogout(jwt: string) {
    clearExpiryTimer()
    const ms = msUntilExpiry(jwt)
    if (ms != null && ms > 0) {
      expiryTimer = setTimeout(() => {
        logout()
        onExpiry?.()
      }, ms)
    }
  }

  function clearExpiryTimer() {
    if (expiryTimer != null) {
      clearTimeout(expiryTimer)
      expiryTimer = null
    }
  }

  function setAuth(newToken: string, newUsername: string) {
    token.value = newToken
    username.value = newUsername
    localStorage.setItem('token', newToken)
    localStorage.setItem('username', newUsername)
    scheduleLogout(newToken)
  }

  function logout() {
    clearExpiryTimer()
    token.value = null
    username.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('username')
  }

  // Clean up expired token on store init, or schedule auto-logout
  if (token.value && !isTokenValid(token.value)) {
    logout()
  } else if (token.value) {
    scheduleLogout(token.value)
  }

  return { token: readonly(token), username: readonly(username), isLoggedIn, setAuth, logout, onTokenExpired }
})

