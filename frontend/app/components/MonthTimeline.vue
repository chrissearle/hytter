<script setup lang="ts">
// Auto-import registers NuxtLink for use as a template *tag*, but `<component
// :is>` needs a value in script scope. Passing the name as a string does not
// work either - it renders a literal <NuxtLink> element. Hence the explicit
// import, which is what Nuxt documents for the dynamic-component case.
import { NuxtLink } from '#components'
import type { BookingSummary, Hut, HutItem } from '~/types/booking'

const props = defineProps<{
  huts: HutItem[]
  bookings: BookingSummary[]
  label: string
  monthStart: number
  monthEnd: number
  /** Booking detail requires a login, so anonymous visitors get inert blocks. */
  linkable: boolean
}>()

const days = computed(() => daysInRange(props.monthStart, props.monthEnd))

const todayIndex = computed(() =>
  todayIndexInRange(props.monthStart, days.value.length, new Date())
)

function hutBlocks(hut: Hut) {
  return blocksForHut(props.bookings, hut, props.monthStart, props.monthEnd)
}

function isWeekend(day: Date) {
  return day.getUTCDay() === 0 || day.getUTCDay() === 6
}

/**
 * Blocks are placed as a percentage of the track rather than in fixed units, so
 * the day columns can flex to fill whatever width is available. Each day cell is
 * `flex-1` off a zero basis, giving exactly 1/n of the track, which is what
 * keeps these offsets aligned with the grid behind them.
 */
function trackPosition(startIndex: number, span: number) {
  const count = days.value.length
  return {
    left: `calc(${startIndex} / ${count} * 100% + 2px)`,
    width: `calc(${span} / ${count} * 100% - 4px)`
  }
}

const todayOffset = computed(() => `calc(${todayIndex.value} / ${days.value.length} * 100%)`)
</script>

<template>
  <!-- min-w keeps the grid legible on a phone, where it scrolls instead. -->
  <div class="overflow-x-auto rounded-lg border border-default">
    <div class="min-w-[46rem]">
      <!-- Day-of-month header -->
      <div class="flex border-b border-default">
        <div
          class="w-40 shrink-0 border-r border-default py-1.5 pl-3 font-display text-sm capitalize tracking-wide text-highlighted"
        >
          {{ label }}
        </div>
        <div class="flex flex-1">
          <div
            v-for="(day, i) in days"
            :key="i"
            class="flex-1 basis-0 border-r border-muted py-1.5 text-center text-[0.65rem] text-dimmed last:border-r-0"
            :class="[isWeekend(day) ? 'bg-elevated' : '']"
          >
            {{ day.getUTCDate() }}
          </div>
        </div>
      </div>

      <!-- Hut rows -->
      <div v-for="hut in huts" :key="hut.value" class="flex border-b border-muted last:border-b-0">
        <div
          class="flex w-40 shrink-0 items-center gap-2 border-r border-default px-3 py-2.5 text-sm text-default"
        >
          <span class="h-2.5 w-2.5 shrink-0 rounded-full" :class="hutStyle(hut.value).swatch" />
          {{ hut.displayName }}
        </div>

        <div class="relative flex-1" style="height: 3.5rem">
          <div class="absolute inset-0 flex">
            <div
              v-for="(day, i) in days"
              :key="i"
              class="flex-1 basis-0 border-r border-muted"
              :class="[isWeekend(day) ? 'bg-elevated' : '']"
            />
          </div>

          <div
            v-if="todayIndex >= 0"
            class="absolute top-0 z-10 h-full w-px bg-primary"
            :style="{ left: todayOffset }"
          />

          <component
            :is="linkable ? NuxtLink : 'div'"
            v-for="{ booking, startIndex, span } in hutBlocks(hut.value)"
            :key="booking.id"
            :to="linkable ? `/bookings/${booking.id}` : undefined"
            class="absolute top-1.5 flex flex-col justify-center overflow-hidden rounded-md px-2 py-1 leading-tight shadow-sm"
            :class="[
              linkable ? 'transition hover:brightness-110' : '',
              hutBlockClass(booking.hut, booking.status === 'APPROVED')
            ]"
            :style="{ ...trackPosition(startIndex, span), height: '2.75rem' }"
            :title="`${hut.displayName} · ${booking.name} · ${booking.arrivalDate} – ${booking.departureDate}`"
          >
            <span class="truncate text-[0.6rem] uppercase tracking-wide opacity-80">
              {{ hut.displayName }}
            </span>
            <span class="truncate text-xs font-medium">{{ booking.name }}</span>
          </component>
        </div>
      </div>
    </div>
  </div>
</template>
