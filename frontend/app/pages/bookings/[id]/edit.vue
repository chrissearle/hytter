<script setup lang="ts">
import type { Booking, BookingInput } from '~/types/booking'

const route = useRoute()
const router = useRouter()
const id = route.params.id as string

definePageMeta({ middleware: 'authenticated' })

const { data: booking, status, error: fetchError } = useBooking(id)

// canEdit is resolved by the backend against the session. Redirect rather than
// let someone fill in a form the API will refuse to save.
watchEffect(() => {
  if (booking.value && !booking.value.canEdit) {
    navigateTo(`/bookings/${id}`)
  }
})

const submitting = ref(false)
const submitError = ref<string | null>(null)

useSeoMeta({
  title: 'Hytter — Rediger booking',
  description: 'Rediger en booking-forespørsel.'
})

async function onSubmit(input: BookingInput) {
  submitting.value = true
  submitError.value = null
  try {
    await $fetch<Booking>(`/api/bookings/${id}`, { method: 'PUT', body: input })
    await clearNuxtData([bookingCacheKey(id), BOOKINGS_CACHE_KEY])
    await router.push(`/bookings/${id}`)
  } catch {
    submitError.value = 'Klarte ikke å lagre endringer. Sjekk at alle felt er fylt ut riktig.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="mx-auto max-w-2xl px-4 py-8 sm:px-6">
    <NuxtLink :to="`/bookings/${id}`" class="text-sm text-primary hover:underline">
      ← Tilbake til booking
    </NuxtLink>

    <h1 class="mt-4 mb-6 font-display text-2xl text-highlighted">Rediger booking</h1>

    <UAlert
      v-if="fetchError"
      color="error"
      variant="subtle"
      title="Klarte ikke å hente booking"
      :description="fetchError.message"
    />

    <div
      v-else-if="status === 'pending'"
      class="flex h-40 items-center justify-center text-sm text-dimmed"
    >
      Henter booking …
    </div>

    <template v-else-if="booking">
      <UAlert
        v-if="submitError"
        color="error"
        variant="subtle"
        title="Noe gikk galt"
        :description="submitError"
        class="mb-4"
      />

      <BookingForm :booking="booking" :loading="submitting" @submit="onSubmit" />
    </template>
  </div>
</template>
