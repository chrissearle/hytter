// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  modules: ['@nuxt/eslint', '@nuxt/ui', '@nuxt/scripts'],

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

  // Infrastructure only: app/plugins/umami.client.ts loads the tracker with
  // the deployment's settings. Without this entry the module bundles Umami
  // Cloud's script at build time and proxies its sends to Umami Cloud, which
  // overrides the self-hosted src
  scripts: {
    registry: {
      umamiAnalytics: { trigger: false, proxy: false, bundle: false }
    }
  },

  runtimeConfig: {
    // Umami, served to the client by server/api/_analytics.get.ts. Set only by
    // the production deployment (NUXT_UMAMI_WEBSITE_ID, NUXT_UMAMI_DOMAINS as
    // a JSON array, NUXT_UMAMI_SRC); left empty, no tracker loads
    umami: {
      websiteId: '',
      domains: [] as string[],
      src: ''
    },
    // Prometheus scrapes this port in-cluster; the ingress only routes 3000.
    // Override with NUXT_METRICS_PORT
    metricsPort: 9090,
    public: {
      imageTag: ''
    }
  },

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
