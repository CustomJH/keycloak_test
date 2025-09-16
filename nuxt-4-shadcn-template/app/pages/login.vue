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

      <!-- Biometric Login (if available) -->
      <div v-if="biometricAvailable" class="space-y-4">
        <TossButton
          variant="outline"
          size="lg"
          :loading="biometricLoading"
          @click="handleBiometricLogin"
          class="w-full"
        >
          <component :is="FingerprintIcon" class="mr-2 h-5 w-5" />
          {{ $t('auth.loginWithBiometric') }}
        </TossButton>

        <div class="relative">
          <div class="absolute inset-0 flex items-center">
            <div class="w-full border-t border-gray-200" />
          </div>
          <div class="relative flex justify-center text-xs uppercase">
            <span class="bg-white px-2 text-gray-500">{{ $t('common.or') }}</span>
          </div>
        </div>
      </div>

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

        <!-- Remember Me & Forgot Password -->
        <div class="flex items-center justify-between">
          <label class="flex items-center space-x-2 cursor-pointer">
            <input 
              v-model="form.rememberMe"
              type="checkbox" 
              class="w-4 h-4 text-blue-600 bg-gray-100 border-gray-300 rounded focus:ring-blue-500 focus:ring-2"
            />
            <span class="text-sm text-gray-700">{{ $t('auth.rememberMe') }}</span>
          </label>
          
          <NuxtLink 
            to="/auth/forgot-password" 
            class="text-sm text-blue-600 hover:text-blue-700 hover:underline"
          >
            {{ $t('auth.forgotPassword') }}
          </NuxtLink>
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

      <!-- Social Login -->
      <div class="space-y-4">
        <div class="relative">
          <div class="absolute inset-0 flex items-center">
            <div class="w-full border-t border-gray-200" />
          </div>
          <div class="relative flex justify-center text-xs uppercase">
            <span class="bg-white px-2 text-gray-500">{{ $t('common.or') }}</span>
          </div>
        </div>

        <div class="grid grid-cols-2 gap-3">
          <TossButton
            variant="outline"
            @click="handleSocialLogin('kakao')"
            class="w-full"
          >
            <div class="w-5 h-5 mr-2 bg-yellow-400 rounded-full flex items-center justify-center">
              <span class="text-xs font-bold text-yellow-900">K</span>
            </div>
            {{ $t('social.kakao') }}
          </TossButton>

          <TossButton
            variant="outline"
            @click="handleSocialLogin('google')"
            class="w-full"
          >
            <component :is="GoogleIcon" class="w-5 h-5 mr-2" />
            {{ $t('social.google') }}
          </TossButton>
        </div>
      </div>

      <!-- Sign Up Link -->
      <div class="text-center pt-4">
        <p class="text-sm text-gray-600">
          {{ $t('auth.dontHaveAccount') }}
          <NuxtLink 
            to="/register" 
            class="font-medium text-blue-600 hover:text-blue-700 hover:underline ml-1"
          >
            {{ $t('auth.createAccount') }}
          </NuxtLink>
        </p>
      </div>
    </div>
  </NuxtLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Fingerprint } from 'lucide-vue-next'
import TossButton from '@/components/toss/TossButton.vue'
import TossInput from '@/components/toss/TossInput.vue'
import { useAuth } from '@/composables/useAuth'
import { loginSchema } from '@/utils/validators'

const FingerprintIcon = Fingerprint
const GoogleIcon = () => h('svg', {
  viewBox: '0 0 24 24',
  class: 'w-5 h-5'
}, [
  h('path', {
    fill: '#4285f4',
    d: 'M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z'
  }),
  h('path', {
    fill: '#34a853',
    d: 'M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z'
  }),
  h('path', {
    fill: '#fbbc05',
    d: 'M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z'
  }),
  h('path', {
    fill: '#ea4335',
    d: 'M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z'
  })
])

// Composables
const { login, loginWithBiometric, isLoading, error, clearError, isBiometricAvailable } = useAuth()
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

const biometricAvailable = ref(false)
const biometricLoading = ref(false)

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

const handleBiometricLogin = async () => {
  biometricLoading.value = true
  
  try {
    const success = await loginWithBiometric()
    if (success) {
      await navigateTo('/')
    }
  } catch (err) {
    console.error('Biometric login error:', err)
  } finally {
    biometricLoading.value = false
  }
}

const handleSocialLogin = async (provider: string) => {
  // Implement social login
  window.location.href = `/api/auth/${provider}`
}

// Lifecycle
onMounted(async () => {
  biometricAvailable.value = await isBiometricAvailable()
})

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