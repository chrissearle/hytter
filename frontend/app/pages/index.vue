<script setup lang="ts">
const huts = useHuts()

const year = new Date().getFullYear()
const seasonStart = `${year}-06-01`
const seasonEnd = `${year}-08-31`

const { data: bookings, status, error } = useBookings(seasonStart, seasonEnd)

useSeoMeta({
  title: 'Hytter — Booking-kalender',
  description: 'Se ledige og reserverte datoer for Huldrebakken, Trollhaugen og telt/hengekøye.'
})
</script>

<template>
  <div class="mx-auto max-w-5xl px-4 py-8 sm:px-6">
    <header class="mb-6">
      <p class="font-display text-sm uppercase tracking-[0.2em] text-ember-600 dark:text-ember-400">
        Sesong {{ year }}
      </p>
      <h1 class="mt-1 font-display text-3xl text-forest-900 dark:text-birch-50">
        Booking-kalender
      </h1>
      <p class="mt-2 max-w-2xl text-sm text-forest-700 dark:text-birch-300">
        Oversikt over ønskede og godkjente opphold på hyttene, 1. juni – 31. august. Fylte felt er
        godkjent, stiplede felt er under vurdering.
      </p>
    </header>

    <UAlert
      v-if="error"
      color="error"
      variant="subtle"
      title="Klarte ikke å hente bookinger"
      :description="error.message"
      class="mb-4"
    />

    <div
      v-else-if="status === 'pending'"
      class="flex h-40 items-center justify-center text-sm text-forest-500"
    >
      Henter bookinger …
    </div>

    <HutTimeline
      v-else
      :huts="huts"
      :bookings="bookings ?? []"
      :season-start="seasonStart"
      :season-end="seasonEnd"
    />

    <div class="mt-4 flex flex-wrap items-center gap-4 text-xs text-forest-600 dark:text-birch-300">
      <span class="flex items-center gap-1.5">
        <span class="h-3 w-3 rounded-sm bg-ember-500" /> Godkjent
      </span>
      <span class="flex items-center gap-1.5">
        <span
          class="h-3 w-3 rounded-sm border border-dashed border-forest-400 bg-forest-50 dark:bg-forest-900"
        />
        Forespurt
      </span>
      <span class="flex items-center gap-1.5"> <span class="h-3 w-px bg-ember-500" /> I dag </span>
    </div>
  </div>
</template>
