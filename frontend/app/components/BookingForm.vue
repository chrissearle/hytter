<script setup lang="ts">
import type { FormError, FormSubmitEvent } from '@nuxt/ui'
import type { Booking, BookingInput } from '~/types/booking'

const props = defineProps<{
  booking?: Booking
  loading?: boolean
}>()

const emit = defineEmits<{
  submit: [input: BookingInput]
}>()

const huts = useHuts()
const { data: session } = useSession()

const namePresets = NAME_PRESETS

const namePreset = ref<NamePreset>(presetForName(props.booking?.name))
const freeText = ref(namePreset.value === 'Annet' ? (props.booking?.name ?? '') : '')
const personligText = ref(
  namePreset.value === 'Personlig' && !session.value?.authenticated
    ? (props.booking?.name ?? '')
    : ''
)

// "Personlig" resolves to the logged-in user's display name automatically; an
// anonymous visitor booking for themselves has no session name to draw on, so
// they get a free-text field instead. Either way this is the value that ends
// up in the booking's `name` field, distinct from the dropdown selection.
const effectiveName = computed(() => {
  if (namePreset.value === 'Personlig') {
    return session.value?.authenticated ? (session.value.name ?? '') : personligText.value
  }
  if (namePreset.value === 'Annet') {
    return freeText.value
  }
  return namePreset.value
})

const state = reactive({
  numberOfPeople: props.booking?.numberOfPeople ?? 1,
  hutId: props.booking?.hutId ?? huts[0]!.id,
  arrivalDate: props.booking?.arrivalDate ?? '',
  departureDate: props.booking?.departureDate ?? ''
})

function validate(): FormError[] {
  return validateBookingForm({ name: effectiveName.value, ...state })
}

function onSubmit(event: FormSubmitEvent<typeof state>) {
  emit('submit', {
    name: effectiveName.value.trim(),
    numberOfPeople: event.data.numberOfPeople,
    hutId: event.data.hutId,
    arrivalDate: event.data.arrivalDate,
    departureDate: event.data.departureDate
  })
}
</script>

<template>
  <UForm :state="state" :validate="validate" class="flex flex-col gap-4" @submit="onSubmit">
    <UFormField label="Navn" name="name">
      <USelect v-model="namePreset" :items="[...namePresets]" class="w-full" />
    </UFormField>

    <UFormField v-if="namePreset === 'Annet'" label="Angi navn" name="name">
      <UInput v-model="freeText" :maxlength="NAME_MAX_LENGTH" class="w-full" placeholder="Navn" />
    </UFormField>

    <UFormField
      v-else-if="namePreset === 'Personlig' && !session?.authenticated"
      label="Ditt navn"
      name="name"
    >
      <UInput
        v-model="personligText"
        :maxlength="NAME_MAX_LENGTH"
        class="w-full"
        placeholder="Navn"
      />
    </UFormField>

    <p v-else-if="namePreset === 'Personlig'" class="text-sm text-forest-600 dark:text-birch-300">
      Booking registreres som <strong>{{ session?.name }}</strong>
    </p>

    <UFormField label="Antall personer" name="numberOfPeople">
      <UInputNumber v-model="state.numberOfPeople" :min="1" class="w-full" />
    </UFormField>

    <UFormField label="Hytte" name="hutId">
      <USelect
        v-model="state.hutId"
        :items="huts.map((hut) => ({ label: hut.name, value: hut.id }))"
        value-key="value"
        class="w-full"
      />
    </UFormField>

    <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
      <UFormField label="Ankomst" name="arrivalDate">
        <UInput v-model="state.arrivalDate" type="date" class="w-full" />
      </UFormField>

      <UFormField label="Avreise" name="departureDate">
        <UInput v-model="state.departureDate" type="date" class="w-full" />
      </UFormField>
    </div>

    <div class="mt-2 flex justify-end gap-2">
      <UButton type="submit" color="primary" :loading="loading">
        {{ booking ? 'Lagre endringer' : 'Send forespørsel' }}
      </UButton>
    </div>
  </UForm>
</template>
