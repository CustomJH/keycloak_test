<template>
  <button
    :type="type"
    :class="cn(buttonVariants({ variant, size, class: props.class }))"
    :disabled="disabled"
    @click="$emit('click', $event)"
  >
    <component
      v-if="loading"
      :is="LoaderIcon"
      class="mr-2 h-4 w-4 animate-spin"
    />
    <slot />
  </button>
</template>

<script setup lang="ts">
import { type VariantProps, cva } from 'class-variance-authority'
import { Loader2 } from 'lucide-vue-next'
import { cn } from '@/lib/utils'

const LoaderIcon = Loader2

const buttonVariants = cva(
  'inline-flex items-center justify-center rounded-lg text-sm font-medium transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50',
  {
    variants: {
      variant: {
        default: 'bg-blue-600 text-white hover:bg-blue-700 hover:shadow-lg hover:-translate-y-0.5 active:translate-y-0 focus-visible:ring-blue-500',
        destructive:
          'bg-red-500 text-red-50 hover:bg-red-600 focus-visible:ring-red-500',
        outline:
          'border border-input bg-background hover:bg-accent hover:text-accent-foreground',
        secondary:
          'bg-gray-100 text-gray-900 hover:bg-gray-200 focus-visible:ring-gray-500',
        ghost: 'hover:bg-accent hover:text-accent-foreground',
        link: 'text-blue-600 underline-offset-4 hover:underline',
      },
      size: {
        default: 'h-12 px-6 py-3',
        sm: 'h-9 px-3',
        lg: 'h-14 px-8 py-4',
        icon: 'h-10 w-10',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'default',
    },
  }
)

export interface ButtonProps {
  variant?: VariantProps<typeof buttonVariants>['variant']
  size?: VariantProps<typeof buttonVariants>['size']
  type?: 'button' | 'submit' | 'reset'
  disabled?: boolean
  loading?: boolean
  class?: string
}

const props = withDefaults(defineProps<ButtonProps>(), {
  variant: 'default',
  size: 'default',
  type: 'button',
  disabled: false,
  loading: false,
})

defineEmits<{
  click: [event: MouseEvent]
}>()
</script>

<style scoped>
/* Toss-style button enhancements */
button[variant="default"] {
  background: linear-gradient(135deg, #667EEA 0%, #3B82F6 100%);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

button[variant="default"]:hover {
  box-shadow: 0 10px 25px -3px rgba(59, 130, 246, 0.3);
}
</style>