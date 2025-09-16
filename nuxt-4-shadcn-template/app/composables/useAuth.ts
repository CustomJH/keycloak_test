import { ref, computed } from 'vue'
import type { Ref } from 'vue'

export interface User {
  id: string
  email: string
  name?: string
  phone?: string
  avatar?: string
  verified: boolean
  createdAt: string
}

export interface LoginCredentials {
  emailOrPhone: string
  password: string
  rememberMe?: boolean
}

export interface AuthState {
  user: User | null
  isAuthenticated: boolean
  isLoading: boolean
  error: string | null
}

// Global auth state
const authState: Ref<AuthState> = ref({
  user: null,
  isAuthenticated: false,
  isLoading: false,
  error: null
})

export const useAuth = () => {
  const { $i18n } = useNuxtApp()

  // Computed properties
  const user = computed(() => authState.value.user)
  const isAuthenticated = computed(() => authState.value.isAuthenticated)
  const isLoading = computed(() => authState.value.isLoading)
  const error = computed(() => authState.value.error)

  // Clear error
  const clearError = () => {
    authState.value.error = null
  }

  // Set loading state
  const setLoading = (loading: boolean) => {
    authState.value.isLoading = loading
  }

  // Login function
  const login = async (credentials: LoginCredentials): Promise<boolean> => {
    try {
      setLoading(true)
      clearError()

      const { data } = await $fetch<{ user: User; token: string }>('/api/auth/login', {
        method: 'POST',
        body: credentials
      })

      // Store user data
      authState.value.user = data.user
      authState.value.isAuthenticated = true

      // Store token in cookie if remember me is checked
      if (credentials.rememberMe) {
        const authCookie = useCookie('auth-token', {
          default: () => '',
          maxAge: 60 * 60 * 24 * 30, // 30 days
          secure: true,
          sameSite: 'strict'
        })
        authCookie.value = data.token
      }

      return true
    } catch (err: any) {
      // Handle different error types
      let errorMessage = $i18n.t('auth.loginFailed')
      
      if (err.status === 401) {
        errorMessage = $i18n.t('auth.invalidCredentials')
      } else if (err.status === 423) {
        errorMessage = $i18n.t('auth.accountLocked')
      } else if (err.status === 429) {
        errorMessage = $i18n.t('auth.tooManyAttempts')
      } else if (err.message) {
        errorMessage = err.message
      }

      authState.value.error = errorMessage
      return false
    } finally {
      setLoading(false)
    }
  }

  // Biometric login
  const loginWithBiometric = async (): Promise<boolean> => {
    try {
      setLoading(true)
      clearError()

      // Check if WebAuthn is supported
      if (!window.PublicKeyCredential) {
        throw new Error('Biometric authentication is not supported')
      }

      // Get stored credential options from server
      const { challengeData } = await $fetch<{ challengeData: any }>('/api/auth/biometric/challenge')

      // Create credential
      const credential = await navigator.credentials.get({
        publicKey: challengeData
      }) as PublicKeyCredential

      if (!credential) {
        throw new Error('Biometric authentication failed')
      }

      // Verify credential with server
      const { data } = await $fetch<{ user: User; token: string }>('/api/auth/biometric/verify', {
        method: 'POST',
        body: {
          credential: {
            id: credential.id,
            rawId: Array.from(new Uint8Array(credential.rawId)),
            response: {
              authenticatorData: Array.from(new Uint8Array((credential.response as AuthenticatorAssertionResponse).authenticatorData)),
              clientDataJSON: Array.from(new Uint8Array(credential.response.clientDataJSON)),
              signature: Array.from(new Uint8Array((credential.response as AuthenticatorAssertionResponse).signature)),
              userHandle: (credential.response as AuthenticatorAssertionResponse).userHandle 
                ? Array.from(new Uint8Array((credential.response as AuthenticatorAssertionResponse).userHandle!))
                : null
            }
          }
        }
      })

      authState.value.user = data.user
      authState.value.isAuthenticated = true

      return true
    } catch (err: any) {
      authState.value.error = err.message || 'Biometric authentication failed'
      return false
    } finally {
      setLoading(false)
    }
  }

  // Logout function
  const logout = async (): Promise<void> => {
    try {
      await $fetch('/api/auth/logout', {
        method: 'POST'
      })
    } catch (err) {
      // Even if logout fails on server, clear local state
      console.error('Logout error:', err)
    } finally {
      // Clear local auth state
      authState.value.user = null
      authState.value.isAuthenticated = false
      authState.value.error = null

      // Clear auth cookie
      const authCookie = useCookie('auth-token')
      authCookie.value = ''

      // Redirect to login page
      await navigateTo('/login')
    }
  }

  // Initialize auth state from cookie
  const initAuth = async () => {
    const authCookie = useCookie('auth-token')
    
    if (authCookie.value) {
      try {
        const { data } = await $fetch<{ user: User }>('/api/auth/me', {
          headers: {
            Authorization: `Bearer ${authCookie.value}`
          }
        })
        
        authState.value.user = data.user
        authState.value.isAuthenticated = true
      } catch (err) {
        // Clear invalid token
        authCookie.value = ''
      }
    }
  }

  // Check if biometric is available
  const isBiometricAvailable = async (): Promise<boolean> => {
    try {
      if (!window.PublicKeyCredential) {
        return false
      }

      return await PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable()
    } catch {
      return false
    }
  }

  // Password reset
  const resetPassword = async (email: string): Promise<boolean> => {
    try {
      setLoading(true)
      clearError()

      await $fetch('/api/auth/reset-password', {
        method: 'POST',
        body: { email }
      })

      return true
    } catch (err: any) {
      authState.value.error = err.message || 'Failed to send reset email'
      return false
    } finally {
      setLoading(false)
    }
  }

  return {
    // State
    user,
    isAuthenticated,
    isLoading,
    error,

    // Actions
    login,
    loginWithBiometric,
    logout,
    initAuth,
    resetPassword,
    clearError,
    isBiometricAvailable
  }
}