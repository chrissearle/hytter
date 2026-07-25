<script setup lang="ts">
import type { NuxtError } from '#app'

const props = defineProps<{
  error: NuxtError
}>()

const isNotFound = computed(() => props.error.statusCode === 404)

const title = computed(() => (isNotFound.value ? 'Fant ikke siden' : 'Noe gikk galt'))

const description = computed(() =>
  isNotFound.value
    ? 'Siden du lette etter finnes ikke. Den kan ha blitt slettet, eller lenken kan være feil.'
    : 'Det oppsto en uventet feil. Prøv igjen, eller gå tilbake til kalenderen.'
)

useSeoMeta({ title: `Hytter — ${title.value}` })

function goHome() {
  clearError({ redirect: '/' })
}
</script>

<template>
  <UApp>
    <div class="mx-auto flex min-h-screen max-w-2xl flex-col justify-center px-4 py-8 sm:px-6">
      <p class="font-display text-sm uppercase tracking-[0.2em] text-primary">
        {{ error.statusCode }}
      </p>

      <h1 class="mt-1 font-display text-3xl text-highlighted">
        {{ title }}
      </h1>

      <p class="mt-3 max-w-lg text-sm text-muted">
        {{ description }}
      </p>

      <div class="mt-6">
        <UButton color="primary" @click="goHome">Tilbake til kalenderen</UButton>
      </div>
    </div>
  </UApp>
</template>
