/**
 * Keeps visitors off pages the API would reject anyway, so they get a redirect
 * instead of a bare error. Requires not just a login but an actual grant: the
 * Keycloak realm is shared, so being signed in proves nothing on its own. The
 * backend remains the real gate.
 */
export default defineNuxtRouteMiddleware(async () => {
  const { data: session } = await useSession()

  if (!session.value?.hasAccess) {
    return navigateTo('/')
  }
})
