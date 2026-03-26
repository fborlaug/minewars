<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { api, ApiError } from '@/services/api'
import AuthForm from '@/components/AuthForm.vue'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const username = ref('')
const password = ref('')
const error = ref<string | null>(null)
const loading = ref(false)
const success = computed(() => route.query.registered === 'ok' ? 'Registration successful — please log in.' : null)

async function login() {
  error.value = null
  loading.value = true
  try {
    const data = await api.login(username.value, password.value)
    auth.setAuth(data.token, data.username)
    router.push('/')
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : 'Could not reach server'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthForm
    v-model:username="username"
    v-model:password="password"
    title="Login"
    submit-label="Login"
    loading-label="Logging in…"
    :loading="loading"
    :error="error"
    :success="success"
    @submit="login"
  >
    <p>No account? <RouterLink to="/register">Register</RouterLink></p>
  </AuthForm>
</template>
