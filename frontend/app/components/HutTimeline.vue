<script setup lang="ts">
import type { BookingSummary, HutItem } from '~/types/booking'

const props = defineProps<{
  huts: HutItem[]
  bookings: BookingSummary[]
  seasonStart: string
  seasonEnd: string
  linkable: boolean
}>()

const months = computed(() => buildMonths(props.seasonStart, props.seasonEnd))

// One column width shared by every month, sized to the season's longest month,
// so a day cell is the same width in June (30 days) as in July/August (31).
// Shorter months just leave the trailing space empty rather than stretching.
const columns = computed(() =>
  Math.max(1, ...months.value.map((m) => daysInRange(m.monthStart, m.monthEnd).length))
)
</script>

<template>
  <div class="flex flex-col gap-4">
    <MonthTimeline
      v-for="month in months"
      :key="month.monthStart"
      :label="month.label"
      :month-start="month.monthStart"
      :month-end="month.monthEnd"
      :columns="columns"
      :huts="huts"
      :bookings="bookings"
      :linkable="linkable"
    />
  </div>
</template>
