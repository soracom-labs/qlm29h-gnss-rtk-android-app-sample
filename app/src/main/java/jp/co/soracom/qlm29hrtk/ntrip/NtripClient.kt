package jp.co.soracom.qlm29hrtk.ntrip

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.OutputStream
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.SSLSocket

interface NtripDataSource {
    suspend fun fetchSourceTable(config: NtripConfig): List<MountPoint>
    suspend fun stream(
        config: NtripConfig,
        latestGga: () -> String?,
        onConnected: suspend () -> Unit,
        onRtcm: suspend (ByteArray) -> Unit,
    ): Nothing
}

class NtripClient : NtripDataSource {
    override suspend fun fetchSourceTable(config: NtripConfig): List<MountPoint> = withContext(Dispatchers.IO) {
        val socket = createSocket(config)
        try {
            socket.soTimeout = config.connectTimeoutMillis
            val input = BufferedInputStream(socket.getInputStream())
            val output = socket.getOutputStream()
            output.write(NtripRequestBuilder.sourceTable(config.host, config.username, config.password))
            output.flush()
            val header = readResponseHeader(input)
            val status = header.lineSequence().firstOrNull().orEmpty()
            if (!NtripResponseParser.isSourceTableSuccess(status)) error("Source Table failed: $status")
            val body = ByteArrayOutputStream()
            val chunk = ByteArray(4_096)
            while (body.size() < MAX_SOURCE_TABLE_BYTES) {
                val count = input.read(chunk)
                if (count < 0) break
                body.write(chunk, 0, count)
                if (body.toString(Charsets.ISO_8859_1.name()).contains("ENDSOURCETABLE")) break
            }
            SourceTableParser.parse(body.toString(Charsets.ISO_8859_1.name()))
        } finally {
            socket.close()
        }
    }

    override suspend fun stream(
        config: NtripConfig,
        latestGga: () -> String?,
        onConnected: suspend () -> Unit,
        onRtcm: suspend (ByteArray) -> Unit,
    ): Nothing = withContext(Dispatchers.IO) {
        val socket = createSocket(config)
        try {
            val input = BufferedInputStream(socket.getInputStream())
            val output = socket.getOutputStream()
            output.write(NtripRequestBuilder.stream(config))
            output.flush()
            val response = readResponseHeader(input)
            val status = response.lineSequence().firstOrNull().orEmpty()
            when (val result = NtripResponseParser.classify(status)) {
                NtripConnectResult.Success -> Unit
                is NtripConnectResult.Failure -> error("NTRIP connection failed: ${result.statusLine}")
            }
            onConnected()

            var lastGgaAt = 0L
            val readBuffer = ByteArray(4_096)
            socket.soTimeout = 250
            while (currentCoroutineContext().isActive) {
                currentCoroutineContext().ensureActive()
                val now = System.currentTimeMillis()
                if (now - lastGgaAt >= config.ggaIntervalMillis) {
                    latestGga()?.let { sendGga(output, it) }
                    lastGgaAt = now
                }
                try {
                    val count = input.read(readBuffer)
                    if (count < 0) error("NTRIP stream closed")
                    if (count > 0) onRtcm(readBuffer.copyOf(count))
                } catch (_: java.net.SocketTimeoutException) {
                    delay(1)
                }
            }
            error("NTRIP stream stopped")
        } finally {
            socket.close()
        }
    }

    private fun createSocket(config: NtripConfig): Socket {
        val socket = if (config.tls) SSLSocketFactory.getDefault().createSocket() else Socket()
        socket.connect(InetSocketAddress(config.host, config.port), config.connectTimeoutMillis)
        if (socket is SSLSocket) {
            socket.sslParameters = socket.sslParameters.apply { endpointIdentificationAlgorithm = "HTTPS" }
            socket.startHandshake()
        }
        return socket
    }

    private fun sendGga(output: OutputStream, sentence: String) {
        val normalized = sentence.trimEnd('\r', '\n') + "\r\n"
        output.write(normalized.toByteArray(Charsets.US_ASCII))
        output.flush()
    }

    private fun readResponseHeader(input: BufferedInputStream): String {
        val bytes = ArrayList<Byte>(256)
        while (bytes.size < MAX_HEADER_BYTES) {
            val value = input.read()
            if (value < 0) error("NTRIP response ended before headers")
            bytes += value.toByte()
            val size = bytes.size
            if (size >= 4 && bytes[size - 4] == 13.toByte() && bytes[size - 3] == 10.toByte() &&
                bytes[size - 2] == 13.toByte() && bytes[size - 1] == 10.toByte()
            ) return bytes.toByteArray().toString(Charsets.ISO_8859_1)
        }
        error("NTRIP response headers are too large")
    }

    companion object {
        private const val MAX_HEADER_BYTES = 16_384
        private const val MAX_SOURCE_TABLE_BYTES = 1_048_576
    }
}
