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

// Editing keeps the booking's own type. Otherwise a member defaults to their
// group; a logged-in non-admin without a group can only book Personlig, and
// everyone else (admin, anonymous) starts on the first fixed group.
const defaultNameType = ((): BookingNameType => {
  if (props.booking) return props.booking.nameType
  if (session.value?.group) return session.value.group
  if (session.value?.authenticated && !session.value.isAdmin) return 'PERSONAL'
  return 'OPPHAVET'
})()

const nameTypeItems = computed(() =>
  allowedNameTypes(reference.value, session.value, props.booking?.nameType).map((item) => ({
    label: item.displayName,
    value: item.value
  }))
)

const state = reactive({
  nameType: defaultNameType,
  name: props.booking?.name ?? '',
  numberOfPeople: props.booking?.numberOfPeople ?? 1,
  hut: (props.booking?.hut ?? 'HULDREBAKKEN') as Hut,
  arrivalDate: props.booking?.arrivalDate ?? '',
  departureDate: props.booking?.departureDate ?? ''
})

const isAdmin = computed(() => session.value?.isAdmin ?? false)

// The stored name is derived server-side for every type except these, so the
// free-text field is the only case where what's typed here is what's saved.
const requiresName = computed(() =>
  needsNameInput(
    reference.value,
    state.nameType,
    session.value?.authenticated ?? false,
    isAdmin.value
  )
)

const showsPersonalHint = computed(
  () => state.nameType === 'PERSONAL' && (session.value?.authenticated ?? false) && !isAdmin.value
)

const nameLabel = computed(() =>
  state.nameType === 'PERSONAL' ? 'Hvem gjelder bookingen?' : 'Angi navn'
)

const nameHelp = computed(() =>
  state.nameType === 'PERSONAL' && isAdmin.value
    ? 'Som admin kan du booke på vegne av andre. Ditt eget navn er fylt inn — endre det om bookingen gjelder noen andre.'
    : undefined
)

// Admins get their own name prefilled when they pick "Personlig", so booking for
// themselves stays one click while booking for someone else is just an edit.
// Client-side only: this fires on user interaction, never during SSR.
watch(
  () => [state.nameType, isAdmin.value] as const,
  ([nameType, admin]) => {
    if (nameType === 'PERSONAL' && admin && !state.name) {
      state.name = session.value?.name ?? ''
    }
  }
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
      <USelect v-model="state.nameType" :items="nameTypeItems" value-key="value" class="w-full" />
    </UFormField>

    <UFormField v-if="requiresName" :label="nameLabel" :help="nameHelp" name="name">
      <UInput v-model="state.name" :maxlength="NAME_MAX_LENGTH" class="w-full" placeholder="Navn" />
    </UFormField>

    <p v-else-if="showsPersonalHint" class="text-sm text-muted">
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
