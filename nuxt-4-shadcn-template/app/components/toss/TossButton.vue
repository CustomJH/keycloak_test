<template>
  <button
    :type="type"
    :class="cn(tossButtonVariants({ variant, size }), props.class)"
    :disabled="disabled || loading"
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

const tossButtonVariants = cva(
  'toss-button inline-flex items-center justify-center rounded-lg font-medium transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed',
  {
    variants: {
      variant: {
        primary: 'toss-button-primary bg-blue-600 text-white hover:bg-blue-700 focus:ring-blue-500',
        secondary: 'toss-button-secondary bg-gray-100 text-gray-700 hover:bg-gray-200 focus:ring-gray-500',
        ghost: 'toss-button-ghost text-gray-600 hover:bg-gray-100 focus:ring-gray-500',
        outline: 'border border-gray-300 bg-white text-gray-700 hover:bg-gray-50 focus:ring-blue-500',
        destructive: 'bg-red-500 text-white hover:bg-red-600 focus:ring-red-500',
        link: 'text-blue-600 hover:text-blue-700 underline-offset-4 hover:underline p-0 h-auto',
      },
      size: {
        sm: 'h-9 px-3 py-2 text-sm',
        default: 'h-12 px-6 py-3 text-sm',
        lg: 'h-14 px-8 py-4 text-base',
        icon: 'h-10 w-10',
      },
    },
    defaultVariants: {
      variant: 'primary',
      size: 'default',
    },
  }
)

export interface TossButtonProps {
  variant?: VariantProps<typeof tossButtonVariants>['variant']
  size?: VariantProps<typeof tossButtonVariants>['size']
  type?: 'button' | 'submit' | 'reset'
  disabled?: boolean
  loading?: boolean
  class?: string
}

const props = withDefaults(defineProps<TossButtonProps>(), {
  variant: 'primary',
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
/* Toss-specific button styles */
.toss-button-primary {
  background: linear-gradient(135deg, #667EEA 0%, #3182F6 100%);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.toss-button-primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(49, 130, 246, 0.25);
}

.toss-button-primary:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.toss-button-secondary:hover:not(:disabled) {
  transform: translateY(-0.5px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.toss-button-ghost:hover:not(:disabled) {
  transform: translateY(-0.5px);
}

/* Mobile optimization */
@media (max-width: 640px) {
  .toss-button[data-size="default"] {
    width: 100%;
    padding: 1rem;
  }
}
</style>