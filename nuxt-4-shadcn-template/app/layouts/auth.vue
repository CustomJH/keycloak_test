<template>
  <div class="min-h-screen bg-gradient-to-br from-blue-50 via-white to-indigo-50">
    <!-- Background Pattern -->
    <div class="absolute inset-0 bg-grid-pattern opacity-5"></div>
    
    <!-- Main Content -->
    <div class="relative flex min-h-screen">
      <!-- Left Side - Branding (Hidden on mobile) -->
      <div class="hidden lg:flex lg:w-1/2 xl:w-2/5 bg-gradient-to-br from-blue-600 to-indigo-700 p-12 text-white">
        <div class="flex flex-col justify-between w-full">
          <!-- Logo -->
          <div class="flex items-center">
            <div class="w-8 h-8 bg-white rounded-lg mr-3 flex items-center justify-center">
              <span class="text-blue-600 font-bold text-lg">T</span>
            </div>
            <span class="text-xl font-semibold">{{ $t('common.appName', 'TossStyle') }}</span>
          </div>

          <!-- Main Content - Simplified for login test -->
          <div class="space-y-8">
            <div>
              <h1 class="text-4xl font-bold leading-tight">
                간편한 로그인
              </h1>
              <p class="text-blue-100 text-lg mt-4 leading-relaxed">
                안전하고 빠른 인증 서비스
              </p>
            </div>
          </div>

          <!-- Footer -->
          <div class="text-sm text-blue-200">
            <p>&copy; 2024 {{ $t('common.appName', 'TossStyle') }}. {{ $t('common.allRightsReserved', 'All rights reserved.') }}</p>
          </div>
        </div>
      </div>

      <!-- Right Side - Auth Form -->
      <div class="flex-1 flex items-center justify-center p-6 lg:p-12">
        <div class="w-full max-w-md">
          <!-- Mobile Logo -->
          <div class="flex items-center justify-center mb-8 lg:hidden">
            <div class="w-10 h-10 bg-blue-600 rounded-xl mr-3 flex items-center justify-center">
              <span class="text-white font-bold text-xl">T</span>
            </div>
            <span class="text-2xl font-bold text-gray-900">{{ $t('common.appName', 'TossStyle') }}</span>
          </div>

          <!-- Language Switcher -->
          <div class="flex justify-end mb-6">
            <select 
              v-model="currentLocale" 
              @change="switchLanguage"
              class="text-sm border border-gray-200 rounded-lg px-3 py-2 bg-white focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="ko">한국어</option>
              <option value="en">English</option>
            </select>
          </div>

          <!-- Auth Form Slot -->
          <div class="bg-white rounded-2xl shadow-xl border border-gray-100 p-8">
            <slot />
          </div>

          <!-- Simplified footer - removed help links for basic login test -->
          <div class="mt-6 text-center text-sm text-gray-500">
            <p>간단한 로그인 테스트</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Check } from 'lucide-vue-next'

const CheckIcon = Check
const { $i18n } = useNuxtApp()
const currentLocale = ref($i18n.locale.value)

const switchLanguage = (event: Event) => {
  const target = event.target as HTMLSelectElement
  $i18n.setLocale(target.value)
}

// Set page title
useHead({
  title: 'Authentication - TossStyle',
  meta: [
    { name: 'description', content: 'Secure login to your TossStyle account' }
  ]
})
</script>

<style scoped>
.bg-grid-pattern {
  background-image: 
    linear-gradient(rgba(59, 130, 246, 0.1) 1px, transparent 1px),
    linear-gradient(90deg, rgba(59, 130, 246, 0.1) 1px, transparent 1px);
  background-size: 50px 50px;
}

/* Responsive design for smaller screens */
@media (max-width: 640px) {
  .bg-gradient-to-br {
    background: #f9fafb;
  }
}
</style>