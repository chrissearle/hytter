// Self-hosted Umami, configured by the deployment through /api/_analytics
// (server/api/_analytics.get.ts). No settings, or a host not in the configured
// domains, means no tracker is fetched at all: the tracker checks domains
// itself, but only after it has been downloaded.
//
// The tracker's own pageview can't say the page failed. A per-status event
// with the path and where the visitor came from does: an internal referrer
// points at a page holding a broken link, an external one at an old or
// mistyped link elsewhere
export default defineNuxtPlugin((nuxtApp) => {
  // After client-side navigation document.referrer still names whatever
  // referred the first page load; Vue Router keeps the previous in-site path
  // in history.state.back
  const referrer = (): string => {
    const state: unknown = window.history.state
    if (state && typeof state === 'object' && 'back' in state && typeof state.back === 'string')
      return new URL(state.back, window.location.origin).href
    return document.referrer || '(none)'
  }

  // A link to a path no page matches makes Nuxt reload the document so the
  // server can answer it, but Vue Router still runs afterEach for the
  // cancelled navigation and Nuxt sets a 404 naming the new path while the
  // old page is showing. That one would be reported against the wrong path;
  // the reloaded page reports the real one
  const leaving = (data: unknown): boolean =>
    !!data &&
    typeof data === 'object' &&
    'path' in data &&
    typeof data.path === 'string' &&
    new URL(data.path, window.location.origin).pathname !== window.location.pathname

  const error = useError()

  nuxtApp.hook('app:mounted', async () => {
    const config = await $fetch('/api/_analytics').catch(() => null)
    if (!config || !config.domains.includes(window.location.hostname)) return

    // The await above dropped the Nuxt context the script composable needs.
    // runWithContext is typed for async callbacks too; this one is synchronous
    const umami = nuxtApp.runWithContext(() =>
      useScriptUmamiAnalytics({
        websiteId: config.websiteId,
        domains: config.domains,
        scriptInput: { src: config.src },
        scriptOptions: { trigger: 'onNuxtReady' }
      })
    ) as ReturnType<typeof useScriptUmamiAnalytics>

    // Covers both an error the server rendered (already set when the app
    // mounts) and one a page throws during client-side navigation. Post
    // flush, so Nuxt has already pushed the failed route's URL into history
    watch(
      error,
      (value) => {
        if (!value || leaving(value.data)) return
        umami.proxy.track(`error-${value.statusCode}`, {
          path: window.location.pathname,
          referrer: referrer()
        })
      },
      { immediate: true, flush: 'post' }
    )
  })
})
