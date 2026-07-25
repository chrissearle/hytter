import {
  appendResponseHeader,
  defineEventHandler,
  getRequestHeaders,
  readRawBody,
  setResponseHeader,
  setResponseStatus
} from 'h3'

// Same-origin from the browser's point of view: it only ever talks to this
// frontend. In production the backend has no ingress of its own, so this is
// the in-cluster service address (e.g. http://hytter-backend:8080). Keycloak
// still redirects the browser straight back to /callback on this origin.
const backend = process.env.NUXT_BACKEND_URL || 'http://localhost:8080'

const BACKEND_TIMEOUT_MS = 15000

/**
 * Headers that describe the *upstream* transfer and must not be replayed.
 * fetch has already decoded the body by the time we see it, so the upstream
 * content-encoding and content-length no longer describe what we are sending -
 * forwarding them makes the browser mis-read a decompressed body.
 */
const HOP_BY_HOP_HEADERS = ['content-encoding', 'content-length', 'transfer-encoding', 'connection']

// /version is proxied so the footer can show what is deployed. /metrics,
// /health and /ready deliberately are not - those are for the cluster.
const shouldProxy = (url: string) =>
  (url.startsWith('/api') && !url.startsWith('/api/_')) ||
  url.startsWith('/login') ||
  url.startsWith('/callback') ||
  url.startsWith('/logout') ||
  url.startsWith('/version')

const shouldSkipHeader = (key: string) => {
  const lower = key.toLowerCase()
  return (
    lower === 'set-cookie' ||
    lower.startsWith('access-control-') ||
    HOP_BY_HOP_HEADERS.includes(lower)
  )
}

export default defineEventHandler(async (event) => {
  const url = event.path || ''
  if (!shouldProxy(url)) return

  const targetUrl = backend + url
  const method = event.method || 'GET'

  const headers: Record<string, string> = {}
  for (const [key, value] of Object.entries(getRequestHeaders(event))) {
    if (typeof value === 'string') {
      headers[key] = value
    }
  }
  delete headers.host
  // Our request to the backend is uncompressed and its length is set by fetch.
  delete headers['content-length']

  let body: string | undefined
  if (!['GET', 'HEAD'].includes(method)) {
    body = (await readRawBody(event)) ?? undefined
  }

  let response: Response
  try {
    response = await fetch(targetUrl, {
      method,
      headers,
      body,
      redirect: 'manual',
      signal: AbortSignal.timeout(BACKEND_TIMEOUT_MS)
    })
  } catch (cause) {
    // An unreachable or hanging backend is a gateway failure, not an unhandled
    // rejection. Same error envelope the backend itself uses.
    console.error(`[proxy] ${method} ${url} failed:`, cause)
    setResponseStatus(event, 502)
    setResponseHeader(event, 'content-type', 'application/json')
    return {
      error: {
        status: { value: 502, description: 'Bad Gateway' },
        message: 'Backend unavailable'
      }
    }
  }

  setResponseStatus(event, response.status)

  // Use getSetCookie() to correctly forward multiple Set-Cookie headers
  for (const cookie of response.headers.getSetCookie()) {
    appendResponseHeader(event, 'set-cookie', cookie)
  }

  for (const [key, value] of response.headers.entries()) {
    if (shouldSkipHeader(key)) continue
    setResponseHeader(event, key, value)
  }

  const data = await response.arrayBuffer()
  return Buffer.from(data)
})
