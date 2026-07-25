import { defineEventHandler, readRawBody, send, setResponseStatus } from 'h3'

// Same-origin from the browser's point of view: it only ever talks to this
// frontend. In production the backend has no ingress of its own, so this is
// the in-cluster service address (e.g. http://hytter-backend:8080). Keycloak
// still redirects the browser straight back to /callback on this origin.
const backend = process.env.NUXT_BACKEND_URL || 'http://localhost:8080'

// /version is proxied so the footer can show what is deployed. /metrics,
// /health and /ready deliberately are not - those are for the cluster.
const shouldProxy = (url: string) =>
  (url.startsWith('/api') && !url.startsWith('/api/_')) ||
  url.startsWith('/login') ||
  url.startsWith('/callback') ||
  url.startsWith('/logout') ||
  url.startsWith('/version')

export default defineEventHandler(async (event) => {
  const url = event.node.req.url || ''
  if (!shouldProxy(url)) return

  const targetUrl = backend + url
  const method = event.node.req.method || 'GET'

  const headers: Record<string, string> = {}
  for (const [key, value] of Object.entries(event.node.req.headers)) {
    if (typeof value === 'string') {
      headers[key] = value
    }
  }
  delete headers.host

  let body: string | undefined
  if (!['GET', 'HEAD'].includes(method)) {
    body = (await readRawBody(event)) ?? undefined
  }

  const response = await fetch(targetUrl, {
    method,
    headers,
    body,
    redirect: 'manual'
  })

  setResponseStatus(event, response.status)

  // Use getSetCookie() to correctly forward multiple Set-Cookie headers
  const setCookies = response.headers.getSetCookie()
  for (const cookie of setCookies) {
    event.node.res.appendHeader('set-cookie', cookie)
  }

  for (const [key, value] of response.headers.entries()) {
    if (
      key.toLowerCase() === 'set-cookie' ||
      key.toLowerCase().startsWith('access-control-') ||
      key.toLowerCase() === 'content-encoding'
    ) {
      continue
    }
    event.node.res.setHeader(key, value)
  }

  const data = await response.arrayBuffer()
  return send(event, Buffer.from(data))
})
