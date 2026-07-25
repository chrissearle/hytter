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
      :linkable="linkable"
    />
  </div>
</template>
