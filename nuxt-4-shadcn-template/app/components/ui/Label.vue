<template>
  <label
    :for="htmlFor"
    :class="cn(
      'text-sm font-medium text-gray-700 transition-all duration-200',
      props.class
    )"
  >
    <slot />
  </label>
</template>

<script setup lang="ts">
import { cn } from '@/lib/utils'

export interface LabelProps {
  htmlFor?: string
  class?: string
}

const props = defineProps<LabelProps>()
</script>