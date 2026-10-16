package net.chrissearle.huts.monitoring

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpHeaders
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.core.metrics.Info
import net.chrissearle.huts.config.BuildInfo
import org.slf4j.event.Level
import java.util.UUID

private val logger = KotlinLogging.logger {}

// Scraped in-cluster by Prometheus directly against the backend pod, not
// through the frontend proxy.
fun Application.configureMonitoring() {
    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    install(MicrometerMetrics) {
        registry = appMicrometerRegistry

        meterBinders =
            listOf(
                JvmMemoryMetrics(),
                JvmGcMetrics(),
                ProcessorMetrics(),
            )
    }

    // Same series as the Nuxt apps, so dashboards can show and annotate what
    // is deployed. A Prometheus Info metric rather than a Micrometer gauge:
    // the registry treats _info as reserved and would export a gauge as
    // app_build
    Info
        .builder()
        .name("app_build_info")
        .help("Deployed build, from the image tag; always 1")
        .labelNames("version")
        .register(appMicrometerRegistry.prometheusRegistry)
        .setLabelValues(BuildInfo.version())

    install(CallLogging) {
        level = Level.INFO
        callIdMdc("call-id")
    }

    install(CallId) {
        header(HttpHeaders.XRequestId)
        generate { UUID.randomUUID().toString() }
        verify { callId: String -> callId.isNotEmpty() }
    }

    routing {
        get("/metrics") {
            logger.debug { "Serving metrics" }
            call.respond(appMicrometerRegistry.scrape())
        }
    }
}
