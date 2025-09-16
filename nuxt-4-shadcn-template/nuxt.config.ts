// https://nuxt.com/docs/api/configuration/nuxt-config
import tailwindcss from '@tailwindcss/postcss'
import autoprefixer from 'autoprefixer'

export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  devtools: { enabled: false },

  // CSS configuration
  css: ['~/assets/css/tailwind.css'],

  // Modules
  modules: [
    '@vueuse/nuxt',
    '@nuxtjs/i18n'
  ],

  // Components configuration
  components: [
    {
      path: '~/components/ui',
      extensions: ['.vue'],
      pathPrefix: false,
    },
    {
      path: '~/components/toss',
      extensions: ['.vue'],
      pathPrefix: false,
    },
    {
      path: '~/components',
      extensions: ['.vue'],
      pathPrefix: false,
    },
  ],

  // Internationalization configuration
  i18n: {
    locales: [
      { code: 'ko', file: 'ko.json', name: '한국어' },
      { code: 'en', file: 'en.json', name: 'English' }
    ],
    defaultLocale: 'ko',
    strategy: 'prefix_except_default',
    langDir: './locales',
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
  },

  // Vite configuration to prevent file watching issues
  vite: {
    server: {
      watch: {
        usePolling: true,
        interval: 1000,
        ignored: [
          '**/node_modules/**',
          '**/.git/**',
          '**/dist/**',
          '**/.nuxt/**',
          '**/.output/**',
          '**/coverage/**',
          '**/tmp/**',
          '**/.tmp/**',
          '**/.cache/**',
          '**/.pnpm-store/**',
          '**/pnpm-lock.yaml',
          '**/package-lock.json',
          '**/yarn.lock'
        ]
      }
    },
    optimizeDeps: {
      exclude: ['fsevents']
    },
    css: {
      postcss: {
        plugins: [
          tailwindcss,
          autoprefixer,
        ],
      },
    },
  },

  // Development optimization
  devServer: {
    host: 'localhost',
    port: 3000
  },

  // Nitro configuration for development
  nitro: {
    storage: {
      redis: {
        driver: 'fs',
        base: './.data/db'
      }
    },
    devStorage: {
      redis: {
        driver: 'fs',
        base: './.data/db'
      }
    }
  }
})
