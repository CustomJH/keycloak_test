<template>
  <input
    :id="id"
    :type="type"
    :class="cn(
      'flex h-12 w-full rounded-lg border border-gray-200 bg-white px-4 py-3 text-sm transition-all duration-200 placeholder:text-transparent focus:border-blue-600 focus:outline-none focus:ring-1 focus:ring-blue-600 disabled:cursor-not-allowed disabled:opacity-50',
      props.class
    )"
    :placeholder="placeholder"
    :value="modelValue"
    :disabled="disabled"
    :required="required"
    :autocomplete="autocomplete"
    @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
    @blur="$emit('blur', $event)"
    @focus="$emit('focus', $event)"
  />
</template>

<script setup lang="ts">
import { cn } from '@/lib/utils'

export interface InputProps {
  id?: string
  type?: string
  placeholder?: string
  modelValue?: string
  disabled?: boolean
  required?: boolean
  autocomplete?: string
  class?: string
}

const props = withDefaults(defineProps<InputProps>(), {
  type: 'text',
  disabled: false,
  required: false,
})

defineEmits<{
  'update:modelValue': [value: string]
  blur: [event: FocusEvent]
  focus: [event: FocusEvent]
}>()
</script>