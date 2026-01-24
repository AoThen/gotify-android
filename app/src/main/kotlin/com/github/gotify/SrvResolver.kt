package com.github.gotify

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.tinylog.kotlin.Logger

internal object SrvResolver {

    suspend fun resolveIfEnabled(settings: Settings): String {
        if (!settings.enableSrvLookup) {
            return settings.url
        }

        val originalUrl = settings.originalUrl
        if (originalUrl.isNullOrBlank()) {
            Logger.debug("SRV lookup enabled but no original URL stored")
            return settings.url
        }

        val parsedOriginal = originalUrl.toHttpUrlOrNull()
        if (parsedOriginal == null) {
            Logger.debug("SRV lookup enabled but original URL is invalid: $originalUrl")
            return settings.url
        }

        val domain = parsedOriginal.host!!

        Logger.info("SRV lookup enabled, re-resolving for domain: $domain")

        val srvResult = withContext(Dispatchers.IO) { SrvLookup.lookup(domain) }
        if (srvResult == null) {
            Logger.warn("SRV lookup failed for $domain, using original URL")
            val resolvedUrl = settings.url
            settings.url = originalUrl
            return resolvedUrl
        }
        val resolved = withContext(Dispatchers.IO) {
            SrvLookup.buildResolvedUrl(originalUrl, srvResult)
        }
        if (resolved == null) {
            Logger.warn("Failed to build resolved URL for SRV result, using original URL")
            val resolvedUrl = settings.url
            settings.url = originalUrl
            return resolvedUrl
        }

        Logger.info("SRV re-resolved to: ${srvResult.host}:${srvResult.port}")
        val oldUrl = settings.url
        settings.url = resolved
        return oldUrl
    }

    fun getResolvedUrl(settings: Settings): String {
        if (!settings.enableSrvLookup) {
            return settings.url
        }

        val originalUrl = settings.originalUrl
        if (originalUrl.isNullOrBlank()) {
            return settings.url
        }

        val parsedOriginal = originalUrl.toHttpUrlOrNull()
        if (parsedOriginal == null) {
            return settings.url
        }

        val domain = parsedOriginal.host ?: return settings.url

        val srvResult = SrvLookup.lookup(domain)
        if (srvResult == null) {
            return settings.url
        }

        val resolved = SrvLookup.buildResolvedUrl(originalUrl, srvResult)
        return resolved ?: settings.url
    }
}
