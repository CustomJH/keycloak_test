import { provideSSRWidth } from '@vueuse/core'

export default defineNuxtPlugin((nuxtApp) => {
  // Provide a default width of 1024px for SSR
  provideSSRWidth(1024, nuxtApp.vueApp)
})