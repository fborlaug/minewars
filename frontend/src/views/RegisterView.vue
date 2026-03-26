<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, ApiError } from '@/services/api'
import AuthForm from '@/components/AuthForm.vue'

const router = useRouter()

const username = ref('')
const password = ref('')
const error = ref<string | null>(null)
const loading = ref(false)

async function register() {
  error.value = null
  loading.value = true
  try {
    await api.register(username.value, password.value)
    router.push({ path: '/login', query: { registered: 'ok' } })
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
    title="Register"
    submit-label="Register"
    loading-label="Registering…"
    password-autocomplete="new-password"
    :loading="loading"
    :error="error"
    @submit="register"
  >
    <p>Already have an account? <RouterLink to="/login">Login</RouterLink></p>
  </AuthForm>
</template>
