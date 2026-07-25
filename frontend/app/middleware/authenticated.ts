/**
 * Keeps anonymous visitors off pages the API would reject anyway, so they get a
 * redirect instead of a bare error. The backend remains the actual gate.
 */
export default defineNuxtRouteMiddleware(async () => {
  const { data: session } = await useSession()

  if (!session.value?.authenticated) {
    return navigateTo('/')
  }
})
