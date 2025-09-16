<template>
  <div class="toss-input-container relative">
    <input
      :id="id"
      ref="inputRef"
      :type="currentType"
      :class="cn(
        'toss-input peer w-full px-4 py-3 bg-white border border-gray-200 rounded-lg transition-all duration-200 placeholder-transparent focus:border-blue-600 focus:ring-1 focus:ring-blue-600 disabled:cursor-not-allowed disabled:opacity-50',
        hasValue ? 'pt-6 pb-2' : '',
        error ? 'border-red-500 focus:border-red-500 focus:ring-red-500' : '',
        props.class
      )"
      :placeholder="label"
      :value="modelValue"
      :disabled="disabled"
      :required="required"
      :autocomplete="autocomplete"
      @input="handleInput"
      @blur="handleBlur"
      @focus="handleFocus"
    />
    
    <!-- Floating Label -->
    <label
      v-if="label"
      :for="id"
      :class="cn(
        'toss-label absolute left-3 text-gray-500 transition-all duration-200 pointer-events-none',
        hasValue || isFocused ? 'top-2 text-xs scale-75 text-blue-600 bg-white px-2 -ml-2' : 'top-3 text-sm',
        error ? 'text-red-500' : ''
      )"
    >
      {{ label }}
      <span v-if="required" class="text-red-500">*</span>
    </label>

    <!-- Icon -->
    <div
      v-if="icon"
      class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400"
    >
      <component :is="iconComponent" class="h-5 w-5" />
    </div>

    <!-- Password Toggle -->
    <button
      v-if="showToggle && (type === 'password')"
      type="button"
      class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
      @click="togglePassword"
    >
      <component :is="currentType === 'password' ? EyeOffIcon : EyeIcon" class="h-5 w-5" />
    </button>

    <!-- Error Message -->
    <p v-if="error" class="mt-1 text-sm text-red-500">
      {{ error }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Mail, Lock, User, Phone, Eye, EyeOff } from 'lucide-vue-next'
import { cn } from '@/lib/utils'

const EyeIcon = Eye
const EyeOffIcon = EyeOff

const iconMap = {
  mail: Mail,
  email: Mail,
  lock: Lock,
  password: Lock,
  user: User,
  phone: Phone,
}

export interface TossInputProps {
  id?: string
  type?: 'text' | 'email' | 'password' | 'tel' | 'number'
  label?: string
  icon?: 'mail' | 'email' | 'lock' | 'password' | 'user' | 'phone'
  showToggle?: boolean
  modelValue?: string
  disabled?: boolean
  required?: boolean
  autocomplete?: string
  error?: string
  class?: string
}

const props = withDefaults(defineProps<TossInputProps>(), {
  type: 'text',
  disabled: false,
  required: false,
  showToggle: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  blur: [event: FocusEvent]
  focus: [event: FocusEvent]
}>()

const inputRef = ref<HTMLInputElement>()
const isFocused = ref(false)
const currentType = ref(props.type)

const hasValue = computed(() => {
  return props.modelValue !== undefined && props.modelValue !== null && props.modelValue !== ''
})

const iconComponent = computed(() => {
  return props.icon ? iconMap[props.icon] : null
})

const handleInput = (event: Event) => {
  const target = event.target as HTMLInputElement
  emit('update:modelValue', target.value)
}

const handleFocus = (event: FocusEvent) => {
  isFocused.value = true
  emit('focus', event)
}

const handleBlur = (event: FocusEvent) => {
  isFocused.value = false
  emit('blur', event)
}

const togglePassword = () => {
  currentType.value = currentType.value === 'password' ? 'text' : 'password'
}

// Watch for type changes from parent
watch(() => props.type, (newType) => {
  currentType.value = newType
})
</script>

<style scoped>
.toss-input:focus + .toss-label,
.toss-input:not(:placeholder-shown) + .toss-label {
  @apply -translate-y-6 scale-75 text-blue-600 bg-white px-2;
}
</style>