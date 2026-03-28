<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { api, ApiError } from '@/services/api'

const auth = useAuthStore()
const router = useRouter()
const message = ref<string>('')
const error = ref<string | null>(null)
const loading = ref(true)

onMounted(async () => {
  try {
    message.value = await api.hello(auth.token)
  } catch (e) {
    // Handled by router guard, but just in case
    if (e instanceof ApiError && e.status === 401) {
      auth.logout()
      router.push({ name: 'login' })
      return
    }
    error.value = e instanceof Error ? e.message : 'Failed to reach backend'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <main class="hello-view">
    <h1>🔗 Backend Connection Test 🚧</h1>
    <p v-if="loading">Loading…</p>
    <p v-else-if="error" class="error">❌ {{ error }}</p>
    <p v-else class="success">✅ {{ message }}</p>
  </main>
</template>

<style scoped>
.hello-view {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 2rem;
}

h1 {
  margin-bottom: 1rem;
}

.success {
  color: var(--color-text);
  font-size: 1.25rem;
}

.error {
  color: var(--color-error);
  font-size: 1.25rem;
}
</style>
