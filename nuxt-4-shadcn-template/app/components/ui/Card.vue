<template>
  <div :class="cn('toss-card bg-white rounded-xl shadow-lg border border-gray-100', props.class)">
    <slot />
  </div>
</template>

<script setup lang="ts">
import { cn } from '@/lib/utils'

export interface CardProps {
  class?: string
}

const props = defineProps<CardProps>()
</script>