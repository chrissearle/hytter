<script setup lang="ts">
import type { BookingSummary } from '~/types/booking'

const props = defineProps<{
  huts: { id: number; name: string }[]
  bookings: BookingSummary[]
  label: string
  monthStart: number
  monthEnd: number
}>()

const DAY_MS = 24 * 60 * 60 * 1000

const days = computed(() => {
  const count = Math.round((props.monthEnd - props.monthStart) / DAY_MS) + 1
  return Array.from({ length: count }, (_, i) => new Date(props.monthStart + i * DAY_MS))
})

const todayIndex = computed(() => {
  const now = new Date()
  const today = Date.UTC(now.getFullYear(), now.getMonth(), now.getDate())
  const index = Math.round((today - props.monthStart) / DAY_MS)
  return index >= 0 && index < days.value.length ? index : -1
})

function blocksForHut(hutId: number) {
  return props.bookings
    .filter((b) => b.hutId === hutId)
    .map((booking) => {
      const [ay, am, ad] = booking.arrivalDate.split('-').map(Number)
      const [dy, dm, dd] = booking.departureDate.split('-').map(Number)
      const arrival = Math.max(Date.UTC(ay ?? 0, (am ?? 1) - 1, ad ?? 1), props.monthStart)
      const departure = Math.min(Date.UTC(dy ?? 0, (dm ?? 1) - 1, dd ?? 1), props.monthEnd)
      if (departure < arrival) return null
      const startIndex = Math.round((arrival - props.monthStart) / DAY_MS)
      const nights = Math.max(Math.round((departure - arrival) / DAY_MS), 0)
      return { booking, startIndex, span: nights + 1 }
    })
    .filter((block): block is NonNullable<typeof block> => block !== null)
}

const dayWidthRem = 1.7
</script>

<template>
  <div class="overflow-x-auto rounded-lg border border-forest-200 dark:border-forest-800">
    <div class="min-w-max" :style="{ '--day-w': `${dayWidthRem}rem` }">
      <!-- Day-of-month header -->
      <div class="flex border-b border-forest-200 dark:border-forest-800">
        <div
          class="w-32 shrink-0 border-r border-forest-200 py-1.5 pl-3 font-display text-sm capitalize tracking-wide text-forest-800 dark:border-forest-800 dark:text-birch-100"
        >
          {{ label }}
        </div>
        <div class="flex">
          <div
            v-for="(day, i) in days"
            :key="i"
            class="shrink-0 border-r border-forest-50 py-1.5 text-center text-[0.65rem] text-forest-500 last:border-r-0 dark:border-forest-900/60 dark:text-birch-400"
            :class="[
              day.getUTCDay() === 0 || day.getUTCDay() === 6
                ? 'bg-birch-100/60 dark:bg-forest-950/40'
                : ''
            ]"
            :style="{ width: 'var(--day-w)' }"
          >
            {{ day.getUTCDate() }}
          </div>
        </div>
      </div>

      <!-- Hut rows -->
      <div
        v-for="hut in huts"
        :key="hut.id"
        class="flex border-b border-forest-100 last:border-b-0 dark:border-forest-900"
      >
        <div
          class="flex w-32 shrink-0 items-center border-r border-forest-200 px-3 py-2.5 text-sm text-forest-700 dark:border-forest-800 dark:text-birch-200"
        >
          {{ hut.name }}
        </div>

        <div
          class="relative"
          :style="{ width: `calc(${days.length} * var(--day-w))`, height: '2.75rem' }"
        >
          <div class="absolute inset-0 flex">
            <div
              v-for="(day, i) in days"
              :key="i"
              class="shrink-0 border-r border-forest-50 dark:border-forest-900/60"
              :class="[
                day.getUTCDay() === 0 || day.getUTCDay() === 6
                  ? 'bg-birch-100/60 dark:bg-forest-950/40'
                  : ''
              ]"
              :style="{ width: 'var(--day-w)' }"
            />
          </div>

          <div
            v-if="todayIndex >= 0"
            class="absolute top-0 z-10 h-full w-px bg-ember-500"
            :style="{ left: `calc(${todayIndex} * var(--day-w))` }"
          />

          <NuxtLink
            v-for="{ booking, startIndex, span } in blocksForHut(hut.id)"
            :key="booking.id"
            :to="`/bookings/${booking.id}`"
            class="absolute top-1 flex h-8 items-center truncate rounded-md px-2 text-xs font-medium shadow-sm transition hover:brightness-95"
            :class="
              booking.status === 'APPROVED'
                ? 'bg-ember-500 text-birch-50'
                : 'border border-dashed border-forest-400 bg-forest-50 text-forest-700 dark:bg-forest-900 dark:text-birch-100'
            "
            :style="{
              left: `calc(${startIndex} * var(--day-w) + 2px)`,
              width: `calc(${span} * var(--day-w) - 4px)`
            }"
            :title="`${booking.name} · ${booking.arrivalDate} – ${booking.departureDate}`"
          >
            {{ booking.name }}
          </NuxtLink>
        </div>
      </div>
    </div>
  </div>
</template>
