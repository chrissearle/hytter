<script setup lang="ts">
const { data: reference } = useReference()
const { data: session } = useSession()

const { seasonYear, seasonStart, seasonEnd } = seasonRangeFor(new Date())

const { data: bookings, status, error } = useBookings(seasonStart, seasonEnd)

useSeoMeta({
  title: 'Hytter — Booking-kalender',
  description: 'Se ledige og reserverte datoer for Huldrebakken, Trollhaugen og telt/hengekøye.'
})
</script>

<template>
  <!-- Wider than the form pages: the calendar is the one view that earns the
       width, and this is used on a desktop. 7xl matches UHeader's container. -->
  <div class="mx-auto max-w-7xl px-4 py-8 sm:px-6">
    <header class="mb-6">
      <p class="font-display text-sm uppercase tracking-[0.2em] text-primary">
        Sesong {{ seasonYear }}
      </p>
      <h1 class="mt-1 font-display text-3xl text-highlighted">Booking-kalender</h1>
      <p class="mt-2 max-w-2xl text-sm text-muted">
        Oversikt over ønskede og godkjente opphold på hyttene, 1. juni – 31. august. Hver hytte har
        sin egen farge. Fylte felt er godkjent, stiplede felt er under vurdering.
      </p>
    </header>

    <UAlert
      v-if="session?.authenticated && !session.hasAccess"
      color="warning"
      variant="subtle"
      title="Du har ikke tilgang"
      description="Du er logget inn, men kontoen din har ikke fått tilgang til hyttebooking ennå. Ta kontakt med en administrator."
      class="mb-4"
    />

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
      class="flex h-40 items-center justify-center text-sm text-dimmed"
    >
      Henter bookinger …
    </div>

    <HutTimeline
      v-else
      :huts="reference?.huts ?? []"
      :bookings="bookings ?? []"
      :season-start="seasonStart"
      :season-end="seasonEnd"
      :linkable="session?.hasAccess ?? false"
    />

    <!-- Two independent keys: hue says which hut, fill vs dashes says status. -->
    <div class="mt-4 flex flex-col gap-3 text-xs text-muted sm:flex-row sm:flex-wrap sm:gap-6">
      <div class="flex flex-wrap items-center gap-3">
        <span
          v-for="hut in reference?.huts ?? []"
          :key="hut.value"
          class="flex items-center gap-1.5"
        >
          <span class="h-3 w-3 rounded-sm" :class="hutStyle(hut.value).swatch" />
          {{ hut.displayName }}
        </span>
      </div>

      <div class="flex flex-wrap items-center gap-3">
        <span class="flex items-center gap-1.5">
          <span class="h-3 w-5 rounded-sm bg-zinc-500" /> Godkjent
        </span>
        <span class="flex items-center gap-1.5">
          <span class="h-3 w-5 rounded-sm border-2 border-dashed border-zinc-500 bg-zinc-500/15" />
          Forespurt
        </span>
        <span class="flex items-center gap-1.5"> <span class="h-3 w-px bg-primary" /> I dag </span>
      </div>
    </div>
  </div>
</template>
