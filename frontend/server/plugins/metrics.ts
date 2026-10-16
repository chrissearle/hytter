import { createServer } from 'node:http'
import { collectDefaultMetrics, Gauge, Histogram, register } from '@prometheus-io/client'
import type { H3Event } from 'h3'

// What a request was for. A bounded label rather than the raw path: one series
// per URL would grow without limit as bots probe paths that don't exist
const routeGroup = (path: string): string => {
  if (path.startsWith('/_nuxt/') || path.startsWith('/_fonts/')) return 'asset'
  if (path.startsWith('/api/_')) return 'api'
  // Forwarded to the Ktor backend by server/middleware/proxy.ts, so this is
  // the round trip through it; the backend's own timing is on its dashboard
  if (
    path.startsWith('/api') ||
    path.startsWith('/login') ||
    path.startsWith('/callback') ||
    path.startsWith('/logout') ||
    path.startsWith('/version')
  )
    return 'backend'
  if (path.endsWith('/_payload.json')) return 'payload'
  return 'page'
}

// The path is attacker-chosen: drop control characters so a request can't
// write fake lines into the log, and cap the length
const loggablePath = (path: string): string =>
  // eslint-disable-next-line no-control-regex
  path.replace(/[\u0000-\u001f\u007f]/g, '?').slice(0, 200)

// Node accepts any registered HTTP method; only these say anything useful here
const methodLabel = (method: string): string =>
  ['GET', 'HEAD', 'POST'].includes(method) ? method : 'OTHER'

export default defineNitroPlugin((nitroApp) => {
  // Prerender runs this server too, and dev restarts it on every change
  if (import.meta.prerender || import.meta.dev) return

  const config = useRuntimeConfig()

  collectDefaultMetrics()

  new Gauge({
    name: 'app_build_info',
    help: 'Deployed build, from the image tag; always 1',
    labelNames: ['version']
  }).set({ version: config.public.imageTag || 'development' }, 1)

  const duration = new Histogram({
    name: 'http_server_request_duration_seconds',
    help: 'Time to complete an HTTP request',
    labelNames: ['method', 'route_group', 'status_code'],
    buckets: [0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2.5, 5, 10]
  })

  // The response's finish event, not Nitro's afterResponse hook: that one is
  // skipped on the error path, so 404s and 500s would never be counted
  nitroApp.hooks.hook('request', (event: H3Event) => {
    // Nitro's own in-process fetches during SSR use unenv's mock request,
    // marked the same way Nitro detects them; they aren't visitor traffic
    if ('__unenv__' in event.node.req) return
    const start = performance.now()
    const path = event.path.split('?')[0] ?? ''
    const labels = {
      method: methodLabel(event.method),
      route_group: routeGroup(path)
    }
    event.node.res.once('finish', () => {
      const status = event.node.res.statusCode
      duration.observe(
        { ...labels, status_code: String(status) },
        (performance.now() - start) / 1000
      )
      // Which URL a 404 was for, which the histogram deliberately can't say;
      // the referer tells a broken link apart from a bot probing
      if (status === 404) {
        const referer = event.node.req.headers.referer
        console.warn(
          `[404] ${labels.method} ${loggablePath(path)}` +
            (referer ? ` from ${loggablePath(referer)}` : '')
        )
      }
    })
  })

  const server = createServer((req, res) => {
    if (req.url !== '/metrics') {
      res.writeHead(404).end()
      return
    }
    register
      .metrics()
      .then((body) => res.writeHead(200, { 'Content-Type': register.contentType }).end(body))
      .catch((error: unknown) => res.writeHead(500).end(String(error)))
  })

  // A metrics port problem shouldn't take the site down with it
  server.on('error', (error) =>
    console.error(`[metrics] port ${config.metricsPort}: ${error.message}`)
  )
  server.listen(Number(config.metricsPort))

  nitroApp.hooks.hook('close', () => {
    server.close()
  })
})
