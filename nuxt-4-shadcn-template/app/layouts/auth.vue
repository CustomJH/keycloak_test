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

          <!-- Main Content -->
          <div class="space-y-8">
            <div>
              <h1 class="text-4xl font-bold leading-tight">
                {{ $t('auth.brandTitle', '금융의 모든 것을\n하나의 앱에서') }}
              </h1>
              <p class="text-blue-100 text-lg mt-4 leading-relaxed">
                {{ $t('auth.brandSubtitle', '간편하고 안전한 디지털 금융 서비스를 경험해보세요') }}
              </p>
            </div>

            <!-- Features -->
            <div class="space-y-4">
              <div class="flex items-center space-x-3">
                <div class="w-6 h-6 bg-white/20 rounded-full flex items-center justify-center">
                  <component :is="CheckIcon" class="w-4 h-4" />
                </div>
                <span>{{ $t('auth.feature1', '생체 인증으로 간편 로그인') }}</span>
              </div>
              <div class="flex items-center space-x-3">
                <div class="w-6 h-6 bg-white/20 rounded-full flex items-center justify-center">
                  <component :is="CheckIcon" class="w-4 h-4" />
                </div>
                <span>{{ $t('auth.feature2', '은행 수준의 보안 시스템') }}</span>
              </div>
              <div class="flex items-center space-x-3">
                <div class="w-6 h-6 bg-white/20 rounded-full flex items-center justify-center">
                  <component :is="CheckIcon" class="w-4 h-4" />
                </div>
                <span>{{ $t('auth.feature3', '24/7 고객 지원 서비스') }}</span>
              </div>
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

          <!-- Help Links -->
          <div class="mt-6 text-center space-x-6 text-sm">
            <NuxtLink to="/help" class="text-gray-500 hover:text-blue-600 transition-colors">
              {{ $t('common.help', '도움말') }}
            </NuxtLink>
            <NuxtLink to="/privacy" class="text-gray-500 hover:text-blue-600 transition-colors">
              {{ $t('common.privacy', '개인정보처리방침') }}
            </NuxtLink>
            <NuxtLink to="/terms" class="text-gray-500 hover:text-blue-600 transition-colors">
              {{ $t('common.terms', '이용약관') }}
            </NuxtLink>
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