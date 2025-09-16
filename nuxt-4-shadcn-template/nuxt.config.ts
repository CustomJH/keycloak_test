// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  devtools: { enabled: true },

  // CSS configuration
  css: ['~/assets/css/tailwind.css'],

  // Modules
  modules: [
    '@nuxtjs/tailwindcss',
    'shadcn-nuxt',
    '@vueuse/nuxt',
    '@nuxtjs/i18n'
  ],

  // shadcn-nuxt configuration
  shadcn: {
    prefix: '',
    componentDir: './app/components/ui'
  },

  // Internationalization configuration
  i18n: {
    locales: [
      { code: 'ko', file: 'ko.json', name: '한국어' },
      { code: 'en', file: 'en.json', name: 'English' }
    ],
    defaultLocale: 'ko',
    strategy: 'prefix_except_default',
    langDir: 'locales/',
    detectBrowserLanguage: {
      useCookie: true,
      cookieKey: 'i18n_redirected',
      redirectOn: 'root'
    }
  },

  // TypeScript configuration
  typescript: {
    strict: true
  },

  // App configuration
  app: {
    head: {
      title: 'Toss Style Login',
      meta: [
        { charset: 'utf-8' },
        { name: 'viewport', content: 'width=device-width, initial-scale=1' },
        { name: 'description', content: 'Modern Toss-inspired login page with Nuxt 4' }
      ],
      link: [
        { rel: 'icon', type: 'image/x-icon', href: '/favicon.ico' }
      ]
    }
  },

  // Runtime config for environment variables
  runtimeConfig: {
    // Private keys (only available on server-side)
    jwtSecret: process.env.JWT_SECRET || 'your-secret-key',
    // Public keys (exposed to client-side)
    public: {
      apiBase: process.env.API_BASE_URL || '/api'
    }
  }
})
