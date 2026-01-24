package com.github.gotify

import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.random.Random
import org.tinylog.kotlin.Logger

internal object SrvLookupPure {

    private const val DNS_PORT = 53
    private const val DNS_TIMEOUT_MS = 3000
    private const val DNS_SERVER_1 = "1.1.1.1"
    private const val DNS_SERVER_2 = "8.8.8.8"

    fun lookup(domain: String): SrvResult? {
        if (domain.isBlank()) {
            return null
        }

        val srvDomain = "_gotify._tcp.$domain"
        Logger.info("SRV pure Kotlin lookup for domain: $srvDomain")

        val dnsServers = listOf(DNS_SERVER_1, DNS_SERVER_2)

        for (dnsServer in dnsServers) {
            try {
                val query = buildDnsQuery(srvDomain)
                val response = sendDnsQuery(dnsServer, query)
                val srvResult = parseDnsResponse(response, domain)

                if (srvResult != null) {
                    Logger.info(
                        "SRV record found: host=${srvResult.host}, " +
                            "port=${srvResult.port}, priority=${srvResult.priority}"
                    )
                    return srvResult
                }
            } catch (e: Exception) {
                Logger.debug("DNS query to $dnsServer failed: ${e.message}")
            }
        }

        Logger.warn("SRV lookup failed for domain: $domain (tried ${dnsServers.size} servers)")
        return null
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

    private fun buildDnsQuery(domain: String): ByteArray {
        val transactionId = Random.nextInt(65536).toInt() and 0xFFFF
        val flags = 0x0100

        val output = ByteArrayOutputStream()

        output.write((transactionId ushr 8) and 0xFF)
        output.write(transactionId and 0xFF)
        output.write((flags ushr 8) and 0xFF)
        output.write(flags and 0xFF)
        output.write(0x00)
        output.write(0x01)
        output.write(0x00)
        output.write(0x00)
        output.write(0x00)
        output.write(0x00)
        output.write(0x00)
        output.write(0x00)

        val labels = domain.split(".")
        for (label in labels) {
            output.write(label.length)
            output.write(label.toByteArray(Charsets.US_ASCII))
        }
        output.write(0)

        output.write(0x00)
        output.write(0x21)

        output.write(0x00)
        output.write(0x01)

        return output.toByteArray()
    }

    private fun sendDnsQuery(dnsServer: String, query: ByteArray): ByteArray {
        DatagramSocket().use { socket ->
            socket.soTimeout = DNS_TIMEOUT_MS

            val address = InetAddress.getByName(dnsServer)
            val packet = DatagramPacket(query, query.size, address, DNS_PORT)
            socket.send(packet)

            val buffer = ByteArray(512)
            val response = DatagramPacket(buffer, buffer.size)
            socket.receive(response)

            return buffer.copyOf(response.length)
        }
    }

    private fun parseDnsResponse(response: ByteArray, originalDomain: String): SrvResult? {
        if (response.size < 12) {
            return null
        }

        val reader = ByteReader(response)

        reader.skip(2)
        reader.skip(2)
        val answerCount = reader.readShort().toInt()
        reader.skip(6)

        skipDomainName(reader, response)
        reader.skip(4)

        val srvRecords = mutableListOf<SrvResult>()
        repeat(answerCount) {
            skipDomainName(reader, response)
            val type = reader.readShort().toInt()
            reader.skip(2)
            reader.skip(4)
            val rdLength = reader.readShort().toInt()

            if (type == 33) {
                val priority = reader.readShort().toInt()
                val weight = reader.readShort().toInt()
                val port = reader.readShort().toInt()
                val target = readDomainName(reader, response)

                if (target.isNotBlank()) {
                    srvRecords.add(SrvResult(target, port, priority, weight))
                }
            } else {
                reader.skip(rdLength)
            }
        }

        return selectBestRecord(srvRecords)
    }

    private fun selectBestRecord(records: List<SrvResult>): SrvResult? {
        if (records.isEmpty()) return null
        if (records.size == 1) return records[0]

        val minPriority = records.minOf { it.priority }
        val priorityRecords = records.filter { it.priority == minPriority }

        if (priorityRecords.size == 1) {
            return priorityRecords[0]
        }

        val totalWeight = priorityRecords.sumOf { it.weight }
        if (totalWeight == 0) {
            return priorityRecords.random()
        }

        var random = Random.nextInt(totalWeight)
        for (record in priorityRecords) {
            random -= record.weight
            if (random <= 0) {
                return record
            }
        }

        return priorityRecords.last()
    }

    private fun skipDomainName(reader: ByteReader, data: ByteArray) {
        while (true) {
            val length = reader.readByte().toInt() and 0xFF
            if (length == 0) break
            if ((length and 0xC0) == 0xC0) {
                reader.skip(1)
                break
            }
            reader.skip(length)
        }
    }

    private fun readDomainName(reader: ByteReader, data: ByteArray): String {
        val name = StringBuilder()
        var jumped = false
        var jumpPosition = -1

        while (true) {
            val length = reader.readByte().toInt() and 0xFF
            if (length == 0) break

            if ((length and 0xC0) == 0xC0) {
                if (!jumped) {
                    jumpPosition = reader.position - 2
                }
                val nextByte = reader.readByte().toInt() and 0xFF
                val offset = ((length and 0x3F) shl 8) or nextByte
                reader.jumpTo(offset)
                jumped = true
            } else {
                val bytes = reader.readBytes(length)
                if (name.isNotEmpty()) {
                    name.append(".")
                }
                name.append(String(bytes, Charsets.US_ASCII))
            }
        }

        if (jumped && jumpPosition > 0) {
            reader.jumpTo(jumpPosition + 2)
        }

        return name.toString()
    }

    private class ByteReader(private val data: ByteArray) {
        private var pos = 0

        fun position(): Int = pos

        fun readByte(): Int {
            return data[pos++].toInt() and 0xFF
        }

        fun readShort(): Int {
            val high = data[pos++].toInt() and 0xFF
            val low = data[pos++].toInt() and 0xFF
            return (high shl 8) or low
        }

        fun readBytes(count: Int): ByteArray {
            val result = data.copyOfRange(pos, pos + count)
            pos += count
            return result
        }

        fun skip(count: Int) {
            pos += count
        }

        fun jumpTo(newPos: Int) {
            pos = newPos
        }
    }
}
