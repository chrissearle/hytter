<script setup lang="ts">
const route = useRoute()
const router = useRouter()
const id = route.params.id as string

definePageMeta({ middleware: 'authenticated' })

const { data: booking, status, error } = useBooking(id)
const { data: session } = useSession()
const { data: reference } = useReference()

const actionError = ref<string | null>(null)
const approving = ref(false)
const deleting = ref(false)
const savingNotes = ref(false)

// Reads through to the fetched booking until the admin types, then the draft
// wins. Deliberately not a watcher seeding a ref: Vue does not run watchers
// during SSR, so the textarea would render empty on the server and only fill in
// after hydration. Cleared after a save so it re-syncs with the refetched note.
const notesDraft = ref<string | null>(null)
const adminNotes = computed({
  get: () => notesDraft.value ?? booking.value?.adminNotes ?? '',
  set: (value: string) => {
    notesDraft.value = value
  }
})

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

async function onSaveNotes() {
  savingNotes.value = true
  actionError.value = null
  try {
    await $fetch(`/api/bookings/${id}/notes`, {
      method: 'PATCH',
      body: { adminNotes: adminNotes.value }
    })
    await refreshNuxtData(bookingCacheKey(id))
    notesDraft.value = null
  } catch {
    actionError.value = 'Klarte ikke å lagre notatet.'
  } finally {
    savingNotes.value = false
  }
}

function statusBadge(bookingStatus: string): { label: string; color: 'success' | 'warning' } {
  return bookingStatus === 'APPROVED'
    ? { label: 'Godkjent', color: 'success' }
    : { label: 'Forespurt', color: 'warning' }
}
</script>

<template>
  <div class="mx-auto max-w-2xl px-4 py-8 sm:px-6">
    <NuxtLink to="/" class="text-sm text-primary hover:underline">
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
      class="mt-4 flex h-40 items-center justify-center text-sm text-dimmed"
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
        <h1 class="font-display text-2xl text-highlighted">
          {{ hutDisplayName(reference, booking.hut) }}
        </h1>
        <div class="flex items-center gap-3">
          <UBadge :color="statusBadge(booking.status).color" variant="subtle">
            {{ statusBadge(booking.status).label }}
          </UBadge>
          <UButton
            v-if="booking.canEdit"
            :to="`/bookings/${booking.id}/edit`"
            size="sm"
            variant="soft"
          >
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
          <dt class="text-xs uppercase tracking-wide text-dimmed">Navn</dt>
          <dd class="mt-1 text-default">
            {{ booking.name }}
          </dd>
        </div>

        <div>
          <dt class="text-xs uppercase tracking-wide text-dimmed">Antall personer</dt>
          <dd class="mt-1 text-default">
            {{ booking.numberOfPeople }}
          </dd>
        </div>

        <div>
          <dt class="text-xs uppercase tracking-wide text-dimmed">Ankomst</dt>
          <dd class="mt-1 text-default">
            {{ booking.arrivalDate }}
          </dd>
        </div>

        <div>
          <dt class="text-xs uppercase tracking-wide text-dimmed">Avreise</dt>
          <dd class="mt-1 text-default">
            {{ booking.departureDate }}
          </dd>
        </div>
      </dl>

      <div v-if="session?.isAdmin" class="mt-6">
        <UFormField
          label="Notater fra admin"
          name="adminNotes"
          help="Vises til den som har booket — f.eks. «Du må ha telt/hengekøye de første 2 dagene»."
        >
          <UTextarea
            v-model="adminNotes"
            :rows="3"
            :maxlength="ADMIN_NOTES_MAX_LENGTH"
            class="w-full"
            placeholder="Ingen notater"
          />
        </UFormField>
        <div class="mt-2 flex justify-end">
          <UButton size="sm" variant="soft" :loading="savingNotes" @click="onSaveNotes">
            Lagre notat
          </UButton>
        </div>
      </div>

      <div v-else-if="booking.adminNotes" class="mt-6">
        <dt class="text-xs uppercase tracking-wide text-dimmed">Notater fra admin</dt>
        <dd class="mt-1 whitespace-pre-line text-default">
          {{ booking.adminNotes }}
        </dd>
      </div>
    </div>
  </div>
</template>
