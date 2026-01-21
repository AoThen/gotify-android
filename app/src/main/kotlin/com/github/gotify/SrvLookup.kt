package com.github.gotify

import org.tinylog.kotlin.Logger
import org.xbill.DNS.AAAARecord
import org.xbill.DNS.ARecord
import org.xbill.DNS.DClass
import org.xbill.DNS.Lookup
import org.xbill.DNS.Name
import org.xbill.DNS.Record
import org.xbill.DNS.SRVRecord
import org.xbill.DNS.Type
import kotlin.random.Random

internal data class SrvResult(
    val host: String,
    val port: Int,
    val priority: Int,
    val weight: Int
)

internal object SrvLookup {

    private const val SERVICE_PREFIX = "_gotify._tcp."

    fun lookup(domain: String): SrvResult? {
        if (domain.isBlank()) {
            return null
        }

        val cleanDomain = domain.trim().removePrefix("_gotify._tcp.")
        val srvRecordName = "$SERVICE_PREFIX$cleanDomain"

        return try {
            val lookup = Lookup(srvRecordName, Type.SRV, DClass.IN)
            lookup.setCache(null)
            val records = lookup.run()

            if (lookup.result != Lookup.SUCCESSFUL || records == null || records.isEmpty()) {
                null
            } else {
                val results = mutableListOf<SrvResult>()
                for (record in records) {
                    if (record is SRVRecord) {
                        val target = record.target.toString()
                        val host = if (target.endsWith(".")) target.dropLast(1) else target
                        results.add(
                            SrvResult(
                                host = host,
                                port = record.port,
                                priority = record.priority,
                                weight = record.weight
                            )
                        )
                    }
                }

                if (results.isEmpty()) {
                    null
                } else {
                    selectBestRecord(results)
                }
            }
        } catch (e: Exception) {
            Logger.error(e, "SRV lookup failed for $domain")
            null
        }
    }

    private fun selectBestRecord(results: List<SrvResult>): SrvResult {
        val minPriority = results.minOfOrNull { it.priority } ?: return results[0]
        val priorityRecords = results.filter { it.priority == minPriority }

        return if (priorityRecords.size == 1) {
            priorityRecords[0]
        } else {
            val totalWeight = priorityRecords.sumOf { it.weight }
            if (totalWeight == 0) {
                priorityRecords.random()
            } else {
                var random = (Random.nextDouble() * totalWeight).toInt()
                for (record in priorityRecords) {
                    random -= record.weight
                    if (random <= 0) return record
                }
                priorityRecords.last()
            }
        }
    }

    fun buildResolvedUrl(originalUrl: String, srvResult: SrvResult): String? {
        return try {
            val scheme = if (originalUrl.startsWith("https://")) "https://" else "http://"
            "$scheme${srvResult.host}:${srvResult.port}"
        } catch (e: Exception) {
            Logger.error(e, "Failed to build resolved URL")
            null
        }
    }
}
