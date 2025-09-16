import { z } from 'zod'

// Email validation
export const emailSchema = z
  .string()
  .min(1, 'validation.required')
  .email('validation.email')

// Phone validation (Korean format)
export const phoneSchema = z
  .string()
  .min(1, 'validation.required')
  .regex(/^01[0-9]-?[0-9]{4}-?[0-9]{4}$/, 'validation.invalidPhone')

// Email or phone validation
export const emailOrPhoneSchema = z
  .string()
  .min(1, 'validation.required')
  .refine((value) => {
    // Check if it's a valid email
    const emailResult = emailSchema.safeParse(value)
    if (emailResult.success) return true
    
    // Check if it's a valid phone number
    const phoneResult = phoneSchema.safeParse(value)
    return phoneResult.success
  }, {
    message: 'validation.email'
  })

// Password validation
export const passwordSchema = z
  .string()
  .min(1, 'validation.required')
  .min(8, 'validation.minLength')
  .max(128, 'validation.maxLength')

// Strong password validation
export const strongPasswordSchema = passwordSchema
  .regex(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]/, 'validation.weakPassword')

// Login form schema
export const loginSchema = z.object({
  emailOrPhone: emailOrPhoneSchema,
  password: passwordSchema,
  rememberMe: z.boolean().optional()
})

// Registration form schema
export const registerSchema = z.object({
  email: emailSchema,
  phone: phoneSchema.optional(),
  password: strongPasswordSchema,
  confirmPassword: z.string().min(1, 'validation.required'),
  agreeToTerms: z.boolean().refine(val => val === true, {
    message: 'validation.required'
  })
}).refine((data) => data.password === data.confirmPassword, {
  message: 'validation.passwordMismatch',
  path: ['confirmPassword']
})

// Password reset schema
export const resetPasswordSchema = z.object({
  email: emailSchema
})

// OTP verification schema
export const otpSchema = z.object({
  otp: z
    .string()
    .min(6, 'validation.required')
    .max(6, 'validation.required')
    .regex(/^\d{6}$/, 'otp.invalidCode')
})

// Utility function to get field-specific validation message
export const getFieldError = (field: string, min?: number, max?: number) => {
  return {
    required: `validation.required`,
    email: 'validation.email',
    minLength: min ? `validation.minLength` : 'validation.minLength',
    maxLength: max ? `validation.maxLength` : 'validation.maxLength',
    passwordMismatch: 'validation.passwordMismatch',
    weakPassword: 'validation.weakPassword',
    invalidPhone: 'validation.invalidPhone'
  }
}

// Validation helper for individual fields
export const validateField = <T>(schema: z.ZodSchema<T>, value: T) => {
  const result = schema.safeParse(value)
  return {
    isValid: result.success,
    error: result.success ? null : result.error.issues[0]?.message || 'Invalid input'
  }
}

// Form validation helper
export const validateForm = <T>(schema: z.ZodSchema<T>, data: T) => {
  const result = schema.safeParse(data)
  
  if (result.success) {
    return {
      isValid: true,
      data: result.data,
      errors: {}
    }
  }

  const errors: Record<string, string> = {}
  result.error.issues.forEach((issue) => {
    const path = issue.path[0] as string
    if (path && !errors[path]) {
      errors[path] = issue.message
    }
  })

  return {
    isValid: false,
    data: null,
    errors
  }
}