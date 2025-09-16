<template>
  <NuxtLayout name="auth">
    <div class="space-y-6">
      <!-- Header -->
      <div class="text-center space-y-2">
        <h1 class="text-3xl font-bold text-gray-900">
          {{ $t('auth.welcome') }}
        </h1>
        <p class="text-gray-600">
          {{ $t('auth.welcomeMessage') }}
        </p>
      </div>

      <!-- Biometric Login removed for simple test -->

      <!-- Login Form -->
      <form @submit.prevent="handleLogin" class="space-y-5">
        <!-- Email/Phone Input -->
        <div class="space-y-1">
          <TossInput
            v-model="form.emailOrPhone"
            type="email"
            :label="$t('auth.emailOrPhone')"
            icon="mail"
            required
            :error="errors.emailOrPhone"
            @blur="validateEmailOrPhone"
          />
        </div>

        <!-- Password Input -->
        <div class="space-y-1">
          <TossInput
            v-model="form.password"
            type="password"
            :label="$t('auth.password')"
            icon="lock"
            :show-toggle="true"
            required
            :error="errors.password"
            @blur="validatePassword"
          />
        </div>

        <!-- Remember Me - simplified for basic login test -->
        <div class="flex items-center">
          <label class="flex items-center space-x-2 cursor-pointer">
            <input 
              v-model="form.rememberMe"
              type="checkbox" 
              class="w-4 h-4 text-blue-600 bg-gray-100 border-gray-300 rounded focus:ring-blue-500 focus:ring-2"
            />
            <span class="text-sm text-gray-700">{{ $t('auth.rememberMe') }}</span>
          </label>
        </div>

        <!-- Login Button -->
        <TossButton
          type="submit"
          size="lg"
          :loading="isLoading"
          :disabled="!isFormValid"
          class="w-full"
        >
          {{ $t('auth.login') }}
        </TossButton>

        <!-- Error Message -->
        <div v-if="error" class="p-4 bg-red-50 border border-red-200 rounded-lg">
          <p class="text-sm text-red-600">{{ error }}</p>
        </div>
      </form>

      <!-- Social Login and Sign Up removed for simple login test -->
    </div>
  </NuxtLayout>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import TossButton from '@/components/toss/TossButton.vue'
import TossInput from '@/components/toss/TossInput.vue'
import { useAuth } from '@/composables/useAuth'
import { loginSchema } from '@/utils/validators'

// Composables - simplified for basic login test
const { login, isLoading, error, clearError } = useAuth()
const { $i18n } = useNuxtApp()

// Reactive state
const form = ref({
  emailOrPhone: '',
  password: '',
  rememberMe: false
})

const errors = ref({
  emailOrPhone: '',
  password: ''
})

// Biometric functionality removed for simple login test

// Computed
const isFormValid = computed(() => {
  return form.value.emailOrPhone.trim() && 
         form.value.password.trim() && 
         !errors.value.emailOrPhone && 
         !errors.value.password
})

// Validation methods
const validateEmailOrPhone = () => {
  const result = loginSchema.shape.emailOrPhone.safeParse(form.value.emailOrPhone)
  errors.value.emailOrPhone = result.success ? '' : $i18n.t(result.error.issues[0].message)
}

const validatePassword = () => {
  const result = loginSchema.shape.password.safeParse(form.value.password)
  errors.value.password = result.success ? '' : $i18n.t(result.error.issues[0].message)
}

const validateForm = () => {
  validateEmailOrPhone()
  validatePassword()
  return isFormValid.value
}

// Event handlers
const handleLogin = async () => {
  if (!validateForm()) return

  clearError()
  
  const success = await login({
    emailOrPhone: form.value.emailOrPhone,
    password: form.value.password,
    rememberMe: form.value.rememberMe
  })

  if (success) {
    await navigateTo('/')
  }
}

// Biometric and social login handlers removed for simple login test

// SEO
definePageMeta({
  layout: false,
  title: 'Sign In',
  description: 'Sign in to your account with Toss-style authentication'
})
</script>

<style scoped>
/* Additional component-specific styles */
form input[type="checkbox"] {
  @apply rounded border-gray-300 text-blue-600 shadow-sm focus:border-blue-300 focus:ring focus:ring-blue-200 focus:ring-opacity-50;
}
</style>