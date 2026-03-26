<script setup lang="ts">
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

auth.onTokenExpired(() => router.push({ name: 'login' }))

function logout() {
  auth.logout()
  router.push('/')
}
</script>

<template>
  <header>
    <h1 class="brand">💣 MineWars</h1>
    <nav>
      <RouterLink to="/">Home</RouterLink>
      <template v-if="auth.isLoggedIn">
        <RouterLink to="/hello">Hello</RouterLink>
        <span class="user">{{ auth.username }}</span>
        <button class="nav-btn" @click="logout">Logout</button>
      </template>
      <template v-else>
        <RouterLink to="/login">Login</RouterLink>
        <RouterLink to="/register">Register</RouterLink>
      </template>
    </nav>
  </header>

  <RouterView />
</template>

<style scoped>
header {
  display: flex;
  align-items: center;
  gap: 2rem;
  padding: 1rem 0;
  border-bottom: 1px solid var(--color-border);
  margin-bottom: 2rem;
}

.brand {
  font-size: 1.5rem;
  font-weight: 700;
  white-space: nowrap;
}

nav {
  display: flex;
  align-items: center;
  gap: 1rem;
  font-size: 1rem;
}

.user {
  font-weight: 600;
}

.nav-btn {
  background: none;
  border: none;
  color: inherit;
  cursor: pointer;
  font-size: 1rem;
  padding: 3px;
  text-decoration: underline;
}
</style>
