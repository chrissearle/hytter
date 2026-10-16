// Umami settings for app/plugins/umami.client.ts. Under /api/_ so
// server/middleware/proxy.ts serves it here instead of forwarding it to the
// backend, from this server's own env
// (NUXT_UMAMI_WEBSITE_ID, NUXT_UMAMI_DOMAINS, NUXT_UMAMI_SRC), so no host or
// id is built into the app. A deployment that sets none (local dev, staging)
// answers 204 and loads no tracker. A route rather than public runtime config
// because prerendered pages carry the public config from build time, which a
// pod env var never reaches
export default defineEventHandler(() => {
  const { websiteId, domains, src } = useRuntimeConfig().umami
  if (!websiteId || !src || domains.length === 0) return null
  return { websiteId, domains, src }
})
