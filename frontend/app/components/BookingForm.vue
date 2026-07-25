<script setup lang="ts">
import type { FormError, FormSubmitEvent } from '@nuxt/ui'
import type { Booking, BookingInput, BookingNameType, Hut } from '~/types/booking'

const props = defineProps<{
  booking?: Booking
  loading?: boolean
}>()

const emit = defineEmits<{
  submit: [input: BookingInput]
}>()

const { data: reference } = useReference()
const { data: session } = useSession()

const state = reactive({
  nameType: (props.booking?.nameType ?? 'OPPHAVET') as BookingNameType,
  name: props.booking?.name ?? '',
  numberOfPeople: props.booking?.numberOfPeople ?? 1,
  hut: (props.booking?.hut ?? 'HULDREBAKKEN') as Hut,
  arrivalDate: props.booking?.arrivalDate ?? '',
  departureDate: props.booking?.departureDate ?? ''
})

// The stored name is derived server-side for every type except these, so the
// free-text field is the only case where what's typed here is what's saved.
const requiresName = computed(() =>
  needsNameInput(reference.value, state.nameType, session.value?.authenticated ?? false)
)

const showsPersonalHint = computed(
  () => state.nameType === 'PERSONAL' && (session.value?.authenticated ?? false)
)

function validate(): FormError[] {
  return validateBookingForm({
    name: state.name,
    requiresName: requiresName.value,
    numberOfPeople: state.numberOfPeople,
    arrivalDate: state.arrivalDate,
    departureDate: state.departureDate
  })
}

function onSubmit(event: FormSubmitEvent<typeof state>) {
  emit('submit', {
    nameType: event.data.nameType,
    name: requiresName.value ? event.data.name.trim() : undefined,
    numberOfPeople: event.data.numberOfPeople,
    hut: event.data.hut,
    arrivalDate: event.data.arrivalDate,
    departureDate: event.data.departureDate
  })
}
</script>

<template>
  <UForm :state="state" :validate="validate" class="flex flex-col gap-4" @submit="onSubmit">
    <UFormField label="Navn" name="nameType">
      <USelect
        v-model="state.nameType"
        :items="
          (reference?.nameTypes ?? []).map((item) => ({
            label: item.displayName,
            value: item.value
          }))
        "
        value-key="value"
        class="w-full"
      />
    </UFormField>

    <UFormField v-if="requiresName" label="Angi navn" name="name">
      <UInput v-model="state.name" :maxlength="NAME_MAX_LENGTH" class="w-full" placeholder="Navn" />
    </UFormField>

    <p v-else-if="showsPersonalHint" class="text-sm text-forest-600 dark:text-birch-300">
      Booking registreres som <strong>{{ session?.name }}</strong>
    </p>

    <UFormField label="Antall personer" name="numberOfPeople">
      <UInputNumber v-model="state.numberOfPeople" :min="1" class="w-full" />
    </UFormField>

    <UFormField label="Hytte" name="hut">
      <USelect
        v-model="state.hut"
        :items="
          (reference?.huts ?? []).map((item) => ({ label: item.displayName, value: item.value }))
        "
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
