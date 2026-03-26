<script setup lang="ts">
defineProps<{
  title: string
  submitLabel: string
  loadingLabel: string
  loading: boolean
  error: string | null
  success?: string | null
  passwordAutocomplete?: string
}>()

const username = defineModel<string>('username', { required: true })
const password = defineModel<string>('password', { required: true })

defineEmits<{ submit: [] }>()
</script>

<template>
  <main class="auth-page">
    <p v-if="success" class="success">{{ success }}</p>
    <h2>{{ title }}</h2>
    <form @submit.prevent="$emit('submit')">
      <label for="username">Username</label>
      <input id="username" v-model="username" name="username" type="text" placeholder="Username" autocomplete="username" required />
      <label for="password">Password</label>
      <input id="password" v-model="password" name="password" type="password" placeholder="Password" :autocomplete="passwordAutocomplete ?? 'current-password'" required />
      <button type="submit" :disabled="loading">{{ loading ? loadingLabel : submitLabel }}</button>
      <p v-if="error" class="error">{{ error }}</p>
    </form>
    <div class="auth-footer">
      <slot />
    </div>
  </main>
</template>

<style scoped>
.auth-page {
  max-width: 320px;
  margin: 2rem auto;
}
form {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
label {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
input {
  padding: 0.5rem;
  font-size: 1rem;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  background: var(--color-background-soft);
  color: var(--color-text);
}
button {
  padding: 0.5rem;
  font-size: 1rem;
  cursor: pointer;
}
.error {
  color: var(--color-error);
}
.success {
  padding: 0.5rem;
  text-align: center;
  color: var(--color-success);
  border: 1px solid var(--color-success);
  border-radius: 4px;
  background: color-mix(in srgb, var(--color-success) 8%, transparent);
  margin-bottom: 0.75rem;
}
.auth-footer {
  margin-top: 1rem;
  text-align: center;
}
</style>
