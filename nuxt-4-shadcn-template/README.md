# Nuxt 4 + Toss-Style Login Template

A modern, Toss-inspired login page built with Nuxt 4, shadcn-vue, and Tailwind CSS.

## ✨ Features

- 🎨 **Toss-Inspired Design**: Beautiful, minimalist UI matching Toss's design philosophy
- 🔐 **Complete Authentication**: Login, logout, and JWT-based auth system
- 🌐 **Internationalization**: Korean/English language support
- 📱 **Mobile-First**: Responsive design optimized for mobile devices
- ⚡ **Modern Stack**: Nuxt 4, Vue 3, TypeScript, Tailwind CSS
- 🎭 **shadcn-vue Components**: Beautiful, accessible UI components
- 🔒 **Security**: Secure password handling and JWT tokens
- 💾 **Form Validation**: Zod schema validation with real-time feedback
- 🎪 **Animations**: Smooth micro-interactions and transitions

## 🏗️ Tech Stack

- **Framework**: Nuxt 4
- **UI Library**: shadcn-vue
- **Styling**: Tailwind CSS
- **Language**: TypeScript
- **Validation**: Zod + VeeValidate
- **Icons**: Lucide Vue Next
- **Authentication**: JWT + bcrypt
- **Internationalization**: @nuxtjs/i18n

## 🚀 Quick Start

### Installation

```bash
# Install dependencies
pnpm install

# Start development server
pnpm dev
```

The app will be available at `http://localhost:3000` (or next available port).

### Demo Login Credentials

```
Email: user@example.com
Password: password123

OR

Email: demo@tossstyle.com  
Password: password123
```

## 📁 Project Structure

```
nuxt-4-shadcn-template/
├── app/
│   ├── assets/css/           # Toss design system CSS
│   ├── components/
│   │   ├── ui/               # Base shadcn-vue components  
│   │   └── toss/             # Toss-specific components
│   ├── composables/          # Vue composables (useAuth)
│   ├── layouts/              # Page layouts
│   ├── pages/                # Application pages
│   ├── plugins/              # Nuxt plugins
│   └── utils/                # Utility functions
├── server/api/auth/          # Authentication API endpoints
├── locales/                  # i18n translation files
└── components.json           # shadcn-vue configuration
```

## 🎨 Design System

The project implements a comprehensive Toss-inspired design system:

### Colors
- **Primary**: Toss Blue (`#3182F6`)
- **Gradients**: Subtle blue gradients
- **Neutrals**: Carefully crafted gray scale
- **Semantic**: Success, warning, error colors

### Typography
- **Font Stack**: Pretendard, system fonts
- **Korean Optimization**: `word-break: keep-all`
- **Responsive Scale**: Fluid typography

### Components
- **TossButton**: Animated buttons with hover effects
- **TossInput**: Floating label inputs
- **TossCard**: Soft shadow cards
- **Form Elements**: Accessible, validated forms

## 🔐 Authentication

### Features
- Email/phone number login
- Password validation
- Remember me functionality
- JWT token management
- Secure API endpoints
- Auto-login from stored tokens

### API Endpoints
- `POST /api/auth/login` - User login
- `POST /api/auth/logout` - User logout  
- `GET /api/auth/me` - Get current user

## 🌍 Internationalization

Supports Korean (default) and English with:
- Complete UI translations
- Form validation messages
- Error messages
- Fallback handling

## 🏃‍♂️ Development

```bash
# Install dependencies
pnpm install

# Start dev server
pnpm dev

# Build for production
pnpm build

# Preview production build
pnpm preview
```

## 📦 Deployment

Build the application for production:

```bash
pnpm build
```

The `dist/` folder contains the production-ready application.

## 🎯 Customization

### Colors
Update Toss design tokens in `app/assets/css/tailwind.css`:

```css
:root {
  --toss-blue-600: #3182F6; /* Your primary color */
  /* ... other tokens */
}
```

### Components
Customize Toss components in `app/components/toss/`:
- `TossButton.vue` - Button styles and animations
- `TossInput.vue` - Input field behavior  
- Add new components following the pattern

### Authentication
Modify authentication logic in:
- `app/composables/useAuth.ts` - Client-side auth
- `server/api/auth/` - Server-side endpoints

## 📝 License

This project is open source and available under the MIT License.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
