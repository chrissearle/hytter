// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  modules: ['@nuxt/eslint', '@nuxt/ui'],

  devtools: {
    enabled: true
  },

  css: ['~/assets/css/main.css'],

  colorMode: {
    preference: 'dark',
    fallback: 'dark'
  },

  // Backend address used only by server/middleware/proxy.ts (server-side, never
  // sent to the browser). Set NUXT_BACKEND_URL in each environment, e.g. the
  // in-cluster service address in production.

  compatibilityDate: '2026-06-30',

  // Formatting is owned entirely by Prettier (.prettierrc.json). ESLint's
  // stylistic layer is disabled here because it duplicates and conflicts with
  // Prettier's own formatting decisions (e.g. vue attribute wrapping) — with
  // both enabled, `eslint --fix` and `prettier --write` fight and neither
  // command converges to a passing state on its own.
  eslint: {
    config: {
      stylistic: false
    }
  }
})
