<template>
  <p :class="cn('text-sm text-gray-600', props.class)">
    <slot />
  </p>
</template>

<script setup lang="ts">
import { cn } from '@/lib/utils'

export interface CardDescriptionProps {
  class?: string
}

const props = defineProps<CardDescriptionProps>()
</script>