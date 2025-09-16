<template>
  <div class="min-h-screen bg-gray-50">
    <!-- Navigation -->
    <nav class="bg-white shadow-sm border-b border-gray-200">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between h-16">
          <div class="flex items-center">
            <div class="w-8 h-8 bg-blue-600 rounded-lg mr-3 flex items-center justify-center">
              <span class="text-white font-bold text-lg">T</span>
            </div>
            <span class="text-xl font-semibold text-gray-900">{{ $t('common.appName', 'TossStyle') }}</span>
          </div>

          <div class="flex items-center space-x-4">
            <div v-if="isAuthenticated && user" class="flex items-center space-x-3">
              <div class="w-8 h-8 bg-gray-200 rounded-full flex items-center justify-center">
                <span class="text-sm font-medium text-gray-600">
                  {{ user.name?.charAt(0) || user.email.charAt(0).toUpperCase() }}
                </span>
              </div>
              <span class="text-sm text-gray-700">{{ user.name || user.email }}</span>
              <TossButton variant="ghost" @click="handleLogout">
                {{ $t('auth.logout') }}
              </TossButton>
            </div>
            
            <div v-else>
              <TossButton @click="navigateTo('/login')">
                {{ $t('auth.login') }}
              </TossButton>
            </div>
          </div>
        </div>
      </div>
    </nav>

    <!-- Main Content -->
    <main class="max-w-7xl mx-auto py-12 px-4 sm:px-6 lg:px-8">
      <div v-if="isAuthenticated" class="text-center">
        <h1 class="text-4xl font-bold text-gray-900 mb-4">
          {{ $t('home.welcome', '환영합니다!') }}
        </h1>
        <p class="text-xl text-gray-600 mb-8">
          {{ $t('home.loggedInMessage', '성공적으로 로그인되었습니다.') }}
        </p>
        
        <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mt-12">
          <div class="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
            <div class="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center mb-4">
              <component :is="CreditCardIcon" class="w-6 h-6 text-blue-600" />
            </div>
            <h3 class="text-lg font-semibold text-gray-900 mb-2">
              {{ $t('home.feature1Title', '간편 결제') }}
            </h3>
            <p class="text-gray-600">
              {{ $t('home.feature1Desc', '빠르고 안전한 결제 경험') }}
            </p>
          </div>

          <div class="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
            <div class="w-12 h-12 bg-green-100 rounded-lg flex items-center justify-center mb-4">
              <component :is="PiggyBankIcon" class="w-6 h-6 text-green-600" />
            </div>
            <h3 class="text-lg font-semibold text-gray-900 mb-2">
              {{ $t('home.feature2Title', '자산 관리') }}
            </h3>
            <p class="text-gray-600">
              {{ $t('home.feature2Desc', '똑똑한 가계부와 투자 관리') }}
            </p>
          </div>

          <div class="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
            <div class="w-12 h-12 bg-purple-100 rounded-lg flex items-center justify-center mb-4">
              <component :is="ShieldCheckIcon" class="w-6 h-6 text-purple-600" />
            </div>
            <h3 class="text-lg font-semibold text-gray-900 mb-2">
              {{ $t('home.feature3Title', '안전 보장') }}
            </h3>
            <p class="text-gray-600">
              {{ $t('home.feature3Desc', '은행 수준의 보안 시스템') }}
            </p>
          </div>
        </div>
      </div>

      <div v-else class="text-center">
        <h1 class="text-4xl font-bold text-gray-900 mb-4">
          {{ $t('home.guestWelcome', 'TossStyle에 오신 것을 환영합니다') }}
        </h1>
        <p class="text-xl text-gray-600 mb-8">
          {{ $t('home.guestMessage', '로그인하여 더 많은 기능을 사용해보세요.') }}
        </p>

        <div class="flex flex-col sm:flex-row justify-center gap-4">
          <TossButton size="lg" @click="navigateTo('/login')">
            {{ $t('auth.login') }}
          </TossButton>
          <TossButton variant="outline" size="lg" @click="navigateTo('/register')">
            {{ $t('auth.register') }}
          </TossButton>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { CreditCard, PiggyBank, ShieldCheck } from 'lucide-vue-next'
import TossButton from '@/components/toss/TossButton.vue'
import { useAuth } from '@/composables/useAuth'

const CreditCardIcon = CreditCard
const PiggyBankIcon = PiggyBank
const ShieldCheckIcon = ShieldCheck

// Composables
const { user, isAuthenticated, logout, initAuth } = useAuth()

// Initialize auth on page load
onMounted(async () => {
  await initAuth()
})

const handleLogout = async () => {
  await logout()
}

// SEO
useHead({
  title: 'TossStyle - Modern Financial Experience',
  meta: [
    { name: 'description', content: 'Experience modern digital financial services with TossStyle' }
  ]
})
</script>