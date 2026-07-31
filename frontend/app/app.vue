<script setup lang="ts">
import type { DropdownMenuItem } from '@nuxt/ui'

const { data: session } = useSession()
const { data: buildVersion } = useVersion()

const toast = useToast()

// The session can end while the tab is open - the Keycloak refresh token has a
// hard ceiling. Both the navigation middleware and the 401 handler in
// useApiFetch re-read /api/session, so watching the value catches either route
// and fires exactly once on the transition.
watch(session, (next, previous) => {
  if (!sessionJustExpired(previous, next)) return

  toast.add({
    title: 'Økten er utløpt',
    description: 'Du er logget ut. Logg inn på nytt for å se eller endre bookinger.',
    icon: 'i-lucide-log-out',
    color: 'warning'
  })
})

useHead({
  meta: [{ name: 'viewport', content: 'width=device-width, initial-scale=1' }],
  link: [{ rel: 'icon', href: '/favicon.ico' }],
  htmlAttrs: {
    lang: 'nb'
  }
})

// /login and /logout are backend routes reached through the Nitro proxy, not
// Nuxt pages - they must be real navigations, so `external` is required. A
// client-side route would 404 in the router and never reach Keycloak.
const userMenuItems = computed<DropdownMenuItem[][]>(() => [
  [
    {
      label: 'Logg ut',
      icon: 'i-lucide-log-out',
      onSelect: () => {
        navigateTo('/logout', { external: true })
      }
    }
  ]
])
</script>

<template>
  <UApp>
    <UHeader>
      <template #left>
        <NuxtLink to="/" class="font-display text-lg text-highlighted"> Hytter </NuxtLink>
      </template>

      <template #right>
        <UButton to="/bookings/new" size="sm" variant="soft">Ny booking</UButton>

        <UDropdownMenu v-if="session?.authenticated" :items="userMenuItems">
          <UButton
            size="sm"
            variant="ghost"
            color="neutral"
            icon="i-lucide-user"
            trailing-icon="i-lucide-chevron-down"
          >
            {{ session.name }}
          </UButton>
        </UDropdownMenu>

        <UButton
          v-else
          to="/login"
          external
          size="sm"
          variant="ghost"
          color="neutral"
          icon="i-lucide-log-in"
        >
          Logg inn
        </UButton>

        <UButton
          to="https://github.com/chrissearle/hytter"
          target="_blank"
          rel="noopener"
          size="sm"
          variant="ghost"
          color="neutral"
          icon="i-simple-icons-github"
          aria-label="Kildekode på GitHub"
        />

        <UColorModeButton />
      </template>
    </UHeader>

    <UMain>
      <NuxtPage />
    </UMain>

    <UFooter>
      <template #left>
        <p class="text-sm text-muted">Hytter — booking for Opphavet, Sørkisrampen &amp; venner</p>
      </template>

      <template #right>
        <p v-if="buildVersion" class="font-mono text-xs text-muted">
          {{ buildVersion.version }}
        </p>
      </template>
    </UFooter>
  </UApp>
</template>
