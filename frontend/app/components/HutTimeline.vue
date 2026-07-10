<script setup lang="ts">
import type { BookingSummary } from '~/types/booking'

const props = defineProps<{
  huts: { id: number; name: string }[]
  bookings: BookingSummary[]
  seasonStart: string
  seasonEnd: string
}>()

function toUtcDate(value: string) {
  const parts = value.split('-').map(Number)
  return Date.UTC(parts[0] ?? 0, (parts[1] ?? 1) - 1, parts[2] ?? 1)
}

const months = computed(() => {
  const start = toUtcDate(props.seasonStart)
  const end = toUtcDate(props.seasonEnd)

  const result: { label: string; monthStart: number; monthEnd: number }[] = []
  const cursor = new Date(start)

  while (Date.UTC(cursor.getUTCFullYear(), cursor.getUTCMonth(), 1) <= end) {
    const year = cursor.getUTCFullYear()
    const month = cursor.getUTCMonth()
    const firstOfMonth = Date.UTC(year, month, 1)
    const lastOfMonth = Date.UTC(year, month + 1, 0)

    result.push({
      label: new Date(firstOfMonth).toLocaleDateString('nb-NO', {
        month: 'long',
        year: 'numeric',
        timeZone: 'UTC'
      }),
      monthStart: Math.max(firstOfMonth, start),
      monthEnd: Math.min(lastOfMonth, end)
    })

    cursor.setUTCMonth(cursor.getUTCMonth() + 1)
  }

  return result
})
</script>

<template>
  <div class="flex flex-col gap-4">
    <MonthTimeline
      v-for="month in months"
      :key="month.monthStart"
      :label="month.label"
      :month-start="month.monthStart"
      :month-end="month.monthEnd"
      :huts="huts"
      :bookings="bookings"
    />
  </div>
</template>
