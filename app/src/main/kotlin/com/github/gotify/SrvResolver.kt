package com.github.gotify

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.tinylog.kotlin.Logger

internal object SrvResolver {

    suspend fun resolveIfEnabled(settings: Settings): String = withContext(Dispatchers.IO) {
        if (!settings.enableSrvLookup) {
            return@withContext settings.url
        }

        val originalUrl = settings.originalUrl
        if (originalUrl.isNullOrBlank()) {
            Logger.debug("SRV lookup enabled but no original URL stored")
            return@withContext settings.url
        }

        val parsedOriginal = originalUrl.toHttpUrlOrNull()
        if (parsedOriginal == null) {
            Logger.debug("SRV lookup enabled but original URL is invalid: $originalUrl")
            return@withContext settings.url
        }

        val domain = parsedOriginal.host!!

        Logger.info("SRV lookup enabled, re-resolving for domain: $domain")

        val srvResult = SrvLookup.lookup(domain)
        if (srvResult == null) {
            Logger.warn("SRV lookup failed for $domain, using original URL")
            val resolvedUrl = settings.url
            settings.url = originalUrl
            return@withContext resolvedUrl
        }

        val resolved = SrvLookup.buildResolvedUrl(originalUrl, srvResult)
        if (resolved == null) {
            Logger.warn("Failed to build resolved URL for SRV result, using original URL")
            val resolvedUrl = settings.url
            settings.url = originalUrl
            return@withContext resolvedUrl
        }

        Logger.info("SRV re-resolved to: ${srvResult.host}:${srvResult.port}")
        val oldUrl = settings.url
        settings.url = resolved
        return@withContext oldUrl
    }

    suspend fun getResolvedUrl(settings: Settings): String = withContext(Dispatchers.IO) {
        if (!settings.enableSrvLookup) {
            return@withContext settings.url
        }

        val originalUrl = settings.originalUrl
        if (originalUrl.isNullOrBlank()) {
            return@withContext settings.url
        }

        val parsedOriginal = originalUrl.toHttpUrlOrNull()
        if (parsedOriginal == null) {
            return@withContext settings.url
        }

        val domain = parsedOriginal.host ?: return@withContext settings.url

        val srvResult = SrvLookup.lookup(domain)
        if (srvResult == null) {
            return@withContext settings.url
        }

        val resolved = SrvLookup.buildResolvedUrl(originalUrl, srvResult)
        return@withContext resolved ?: settings.url
    }
}
