import { z } from 'zod'
import jwt from 'jsonwebtoken'
import bcrypt from 'bcrypt'

// Mock user database (in production, use a real database)
const mockUsers = [
  {
    id: '1',
    email: 'user@example.com',
    phone: '010-1234-5678',
    password: '$2b$10$8K1p/a0drtIzU0u.kxk5ButA4HDYgdQmLJkNjpWM6.Ln.5q3K2Rhu', // password: 'password123'
    name: '김토스',
    verified: true,
    createdAt: '2024-01-01T00:00:00Z'
  },
  {
    id: '2', 
    email: 'demo@tossstyle.com',
    phone: '010-9876-5432',
    password: '$2b$10$8K1p/a0drtIzU0u.kxk5ButA4HDYgdQmLJkNjpWM6.Ln.5q3K2Rhu', // password: 'password123'
    name: '데모사용자',
    verified: true,
    createdAt: '2024-01-15T00:00:00Z'
  }
]

// Login request schema
const loginSchema = z.object({
  emailOrPhone: z.string().min(1),
  password: z.string().min(1),
  rememberMe: z.boolean().optional()
})

export default defineEventHandler(async (event) => {
  try {
    // Only allow POST method
    assertMethod(event, 'POST')

    // Parse and validate request body
    const body = await readBody(event)
    const validatedData = loginSchema.parse(body)

    // Find user by email or phone
    const user = mockUsers.find(u => 
      u.email === validatedData.emailOrPhone || 
      u.phone === validatedData.emailOrPhone
    )

    if (!user) {
      throw createError({
        statusCode: 401,
        statusMessage: 'Invalid credentials'
      })
    }

    // Verify password
    const isPasswordValid = await bcrypt.compare(validatedData.password, user.password)
    
    if (!isPasswordValid) {
      throw createError({
        statusCode: 401,
        statusMessage: 'Invalid credentials'
      })
    }

    // Get JWT secret from runtime config
    const runtimeConfig = useRuntimeConfig()
    const jwtSecret = runtimeConfig.jwtSecret

    // Create JWT token
    const token = jwt.sign(
      { 
        userId: user.id,
        email: user.email 
      },
      jwtSecret,
      { 
        expiresIn: validatedData.rememberMe ? '30d' : '24h' 
      }
    )

    // Remove password from user object
    const { password: _, ...userWithoutPassword } = user

    // Return success response
    return {
      success: true,
      data: {
        user: userWithoutPassword,
        token
      }
    }

  } catch (error) {
    // Handle Zod validation errors
    if (error instanceof z.ZodError) {
      throw createError({
        statusCode: 400,
        statusMessage: 'Invalid request data',
        data: error.issues
      })
    }

    // Handle other errors
    if (error.statusCode) {
      throw error
    }

    // Generic server error
    console.error('Login error:', error)
    throw createError({
      statusCode: 500,
      statusMessage: 'Internal server error'
    })
  }
})