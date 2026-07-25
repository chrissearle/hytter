package net.chrissearle.huts.config

const val DEVELOPMENT_VERSION = "development"

private const val IMAGE_TAG_RESOURCE = "/image-tag.txt"

/**
 * The deployed image tag. CI writes `image-tag.txt` into the image before the
 * build (see .github/workflows/ci.yaml); running from source it is simply
 * absent, which is not an error - that is what [DEVELOPMENT_VERSION] means.
 */
object BuildInfo {
    fun version(): String =
        javaClass
            .getResourceAsStream(IMAGE_TAG_RESOURCE)
            ?.bufferedReader()
            ?.use { it.readText() }
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: DEVELOPMENT_VERSION
}
