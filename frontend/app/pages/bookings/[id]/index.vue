<script setup lang="ts">
const route = useRoute()
const id = route.params.id as string

const { data: booking, status, error } = useBooking(id)
const { data: session } = useSession()

const canEdit = computed(
  () =>
    session.value?.isAdmin ||
    (session.value?.authenticated && session.value.name === booking.value?.createdBy)
)

useSeoMeta({
  title: 'Hytter — Booking',
  description: 'Detaljer for en hytte-booking.'
})

// TODO(user): map a booking status to a display label + UColor for the badge.
// Two statuses today (OPEN/APPROVED) but admin notes and future statuses
// (e.g. a rejected/cancelled state) may want distinct colors later — your
// call on how much to generalize now vs. keep it a simple two-way branch.
function statusBadge(bookingStatus: string): { label: string; color: 'success' | 'warning' } {
  return bookingStatus === 'APPROVED'
    ? { label: 'Godkjent', color: 'success' }
    : { label: 'Forespurt', color: 'warning' }
}
</script>

<template>
  <div class="mx-auto max-w-2xl px-4 py-8 sm:px-6">
    <NuxtLink to="/" class="text-sm text-ember-600 hover:underline dark:text-ember-400">
      ← Tilbake til kalenderen
    </NuxtLink>

    <UAlert
      v-if="error"
      color="error"
      variant="subtle"
      title="Klarte ikke å hente booking"
      :description="error.message"
      class="mt-4"
    />

    <div
      v-else-if="status === 'pending'"
      class="mt-4 flex h-40 items-center justify-center text-sm text-forest-500"
    >
      Henter booking …
    </div>

    <div v-else-if="booking" class="mt-4">
      <div class="mb-4 flex items-center justify-between">
        <h1 class="font-display text-2xl text-forest-900 dark:text-birch-50">
          {{ booking.hutName }}
        </h1>
        <div class="flex items-center gap-3">
          <UBadge :color="statusBadge(booking.status).color" variant="subtle">
            {{ statusBadge(booking.status).label }}
          </UBadge>
          <UButton v-if="canEdit" :to="`/bookings/${booking.id}/edit`" size="sm" variant="soft">
            Rediger
          </UButton>
        </div>
      </div>

      <dl class="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div>
          <dt class="text-xs uppercase tracking-wide text-forest-500 dark:text-birch-400">Navn</dt>
          <dd class="mt-1 text-forest-800 dark:text-birch-100">
            {{ booking.name }}
          </dd>
        </div>

        <div>
          <dt class="text-xs uppercase tracking-wide text-forest-500 dark:text-birch-400">
            Antall personer
          </dt>
          <dd class="mt-1 text-forest-800 dark:text-birch-100">
            {{ booking.numberOfPeople }}
          </dd>
        </div>

        <div>
          <dt class="text-xs uppercase tracking-wide text-forest-500 dark:text-birch-400">
            Ankomst
          </dt>
          <dd class="mt-1 text-forest-800 dark:text-birch-100">
            {{ booking.arrivalDate }}
          </dd>
        </div>

        <div>
          <dt class="text-xs uppercase tracking-wide text-forest-500 dark:text-birch-400">
            Avreise
          </dt>
          <dd class="mt-1 text-forest-800 dark:text-birch-100">
            {{ booking.departureDate }}
          </dd>
        </div>
      </dl>

      <div v-if="booking.adminNotes" class="mt-6">
        <dt class="text-xs uppercase tracking-wide text-forest-500 dark:text-birch-400">
          Notater fra admin
        </dt>
        <dd class="mt-1 whitespace-pre-line text-forest-800 dark:text-birch-100">
          {{ booking.adminNotes }}
        </dd>
      </div>
    </div>
  </div>
</template>
