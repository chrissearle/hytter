<script setup lang="ts">
const route = useRoute()
const router = useRouter()
const id = route.params.id as string

const { data: booking, status, error } = useBooking(id)
const { data: session } = useSession()
const { data: reference } = useReference()

const canEdit = computed(
  () =>
    session.value?.isAdmin ||
    (session.value?.authenticated && session.value.name === booking.value?.createdBy)
)

const actionError = ref<string | null>(null)
const approving = ref(false)
const deleting = ref(false)

useSeoMeta({
  title: 'Hytter — Booking',
  description: 'Detaljer for en hytte-booking.'
})

async function onApprove() {
  approving.value = true
  actionError.value = null
  try {
    await $fetch(`/api/bookings/${id}/approve`, { method: 'POST' })
    await clearNuxtData([bookingCacheKey(id), BOOKINGS_CACHE_KEY])
    await refreshNuxtData(bookingCacheKey(id))
  } catch {
    actionError.value = 'Klarte ikke å godkjenne booking.'
  } finally {
    approving.value = false
  }
}

async function onDelete() {
  if (!confirm('Er du sikker på at du vil slette denne bookingen?')) return
  deleting.value = true
  actionError.value = null
  try {
    await $fetch(`/api/bookings/${id}`, { method: 'DELETE' })
    await clearNuxtData(BOOKINGS_CACHE_KEY)
    await router.push('/')
  } catch {
    actionError.value = 'Klarte ikke å slette booking.'
    deleting.value = false
  }
}

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
      <UAlert
        v-if="actionError"
        color="error"
        variant="subtle"
        title="Noe gikk galt"
        :description="actionError"
        class="mb-4"
      />

      <div class="mb-4 flex items-center justify-between">
        <h1 class="font-display text-2xl text-forest-900 dark:text-birch-50">
          {{ hutDisplayName(reference, booking.hut) }}
        </h1>
        <div class="flex items-center gap-3">
          <UBadge :color="statusBadge(booking.status).color" variant="subtle">
            {{ statusBadge(booking.status).label }}
          </UBadge>
          <UButton v-if="canEdit" :to="`/bookings/${booking.id}/edit`" size="sm" variant="soft">
            Rediger
          </UButton>
          <UButton
            v-if="session?.isAdmin && booking.status === 'OPEN'"
            size="sm"
            variant="soft"
            color="success"
            :loading="approving"
            @click="onApprove"
          >
            Godkjenn
          </UButton>
          <UButton
            v-if="session?.isAdmin"
            size="sm"
            variant="soft"
            color="error"
            :loading="deleting"
            @click="onDelete"
          >
            Slett
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
