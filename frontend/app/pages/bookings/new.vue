<script setup lang="ts">
import type { Booking, BookingInput } from '~/types/booking'

const router = useRouter()
const submitting = ref(false)
const error = ref<string | null>(null)

useSeoMeta({
  title: 'Hytter — Ny booking',
  description: 'Registrer en ny booking-forespørsel.'
})

async function onSubmit(input: BookingInput) {
  submitting.value = true
  error.value = null
  try {
    const booking = await $fetch<Booking>('/api/bookings', { method: 'POST', body: input })
    await clearNuxtData(BOOKINGS_CACHE_KEY)
    await router.push(`/bookings/${booking.id}`)
  } catch {
    error.value = 'Klarte ikke å opprette booking. Sjekk at alle felt er fylt ut riktig.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="mx-auto max-w-2xl px-4 py-8 sm:px-6">
    <NuxtLink to="/" class="text-sm text-primary hover:underline">
      ← Tilbake til kalenderen
    </NuxtLink>

    <h1 class="mt-4 mb-6 font-display text-2xl text-highlighted">Ny booking</h1>

    <UAlert
      v-if="error"
      color="error"
      variant="subtle"
      title="Noe gikk galt"
      :description="error"
      class="mb-4"
    />

    <BookingForm :loading="submitting" @submit="onSubmit" />
  </div>
</template>
