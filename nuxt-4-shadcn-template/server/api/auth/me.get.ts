import jwt from 'jsonwebtoken'

// Mock user database (same as in login.post.ts)
const mockUsers = [
  {
    id: '1',
    email: 'user@example.com',
    phone: '010-1234-5678',
    name: '김토스',
    verified: true,
    createdAt: '2024-01-01T00:00:00Z'
  },
  {
    id: '2', 
    email: 'demo@tossstyle.com',
    phone: '010-9876-5432',
    name: '데모사용자',
    verified: true,
    createdAt: '2024-01-15T00:00:00Z'
  }
]

export default defineEventHandler(async (event) => {
  try {
    // Only allow GET method
    assertMethod(event, 'GET')

    // Get authorization header
    const authorization = getHeader(event, 'authorization')
    
    if (!authorization || !authorization.startsWith('Bearer ')) {
      throw createError({
        statusCode: 401,
        statusMessage: 'Missing or invalid authorization header'
      })
    }

    // Extract token
    const token = authorization.substring(7) // Remove 'Bearer ' prefix

    // Get JWT secret from runtime config
    const runtimeConfig = useRuntimeConfig()
    const jwtSecret = runtimeConfig.jwtSecret

    // Verify and decode token
    const decoded = jwt.verify(token, jwtSecret) as { userId: string; email: string }

    // Find user by ID
    const user = mockUsers.find(u => u.id === decoded.userId)

    if (!user) {
      throw createError({
        statusCode: 401,
        statusMessage: 'User not found'
      })
    }

    // Return user data
    return {
      success: true,
      data: {
        user
      }
    }

  } catch (error) {
    // Handle JWT errors
    if (error.name === 'JsonWebTokenError') {
      throw createError({
        statusCode: 401,
        statusMessage: 'Invalid token'
      })
    }

    if (error.name === 'TokenExpiredError') {
      throw createError({
        statusCode: 401,
        statusMessage: 'Token expired'
      })
    }

    // Handle other errors
    if (error.statusCode) {
      throw error
    }

    console.error('Profile fetch error:', error)
    throw createError({
      statusCode: 500,
      statusMessage: 'Internal server error'
    })
  }
})