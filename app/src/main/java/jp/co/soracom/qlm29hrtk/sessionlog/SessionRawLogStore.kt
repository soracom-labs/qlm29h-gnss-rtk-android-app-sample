package jp.co.soracom.qlm29hrtk.sessionlog

import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Persists byte-exact session inputs without blocking the USB receive thread.
 *
 * NMEA and RTCM deliberately remain separate: NMEA is the receiver's replayable
 * result, while RTCM is the correction input used to diagnose how that result
 * was produced. A single ordered command queue prevents bytes delayed by I/O
 * from leaking into the next USB session.
 */
class SessionRawLogStore(private val rootDirectory: File) {
    private val commands = Channel<Command>(Channel.UNLIMITED)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            var active: ActiveSession? = null
            var writeFailure: Throwable? = null
            try {
                for (command in commands) {
                    when (command) {
                        is Command.Start -> {
                            command.result.complete(runCatching {
                                active?.close()
                                writeFailure = null
                                val directory = sessionDirectory(command.sessionId).apply { mkdirs() }
                                active = ActiveSession(
                                    command.sessionId,
                                    FileOutputStream(File(directory, NMEA_FILE_NAME), false),
                                    FileOutputStream(File(directory, RTCM_FILE_NAME), false),
                                )
                            })
                        }
                        is Command.Write -> {
                            val session = active ?: continue
                            runCatching {
                                when (command.stream) {
                                    Stream.NMEA -> session.nmea.write(command.bytes)
                                    Stream.RTCM -> session.rtcm.write(command.bytes)
                                }
                            }.onFailure { error ->
                                writeFailure = error
                                session.close()
                                active = null
                            }
                        }
                        is Command.Finish -> {
                            command.result.complete(runCatching {
                                active?.close()
                                active = null
                                writeFailure?.let { throw it }
                                Unit
                            }.also {
                                writeFailure = null
                            })
                        }
                        is Command.Delete -> {
                            command.result.complete(runCatching {
                                check(active?.id != command.sessionId) { "Cannot delete the active session log" }
                                deleteRecursively(sessionDirectory(command.sessionId))
                            })
                        }
                    }
                }
            } finally {
                active?.close()
            }
        }
    }

    suspend fun startSession(sessionId: String) {
        val result = CompletableDeferred<Result<Unit>>()
        commands.send(Command.Start(sessionId, result))
        result.await().getOrThrow()
    }

    fun appendNmea(bytes: ByteArray) {
        commands.trySend(Command.Write(Stream.NMEA, bytes.copyOf()))
    }

    fun appendRtcm(bytes: ByteArray) {
        commands.trySend(Command.Write(Stream.RTCM, bytes.copyOf()))
    }

    suspend fun finishSession() {
        val result = CompletableDeferred<Result<Unit>>()
        commands.send(Command.Finish(result))
        result.await().getOrThrow()
    }

    suspend fun deleteSession(sessionId: String) {
        val result = CompletableDeferred<Result<Unit>>()
        commands.send(Command.Delete(sessionId, result))
        result.await().getOrThrow()
    }

    fun nmeaFile(sessionId: String): File = File(sessionDirectory(sessionId), NMEA_FILE_NAME)
    fun rtcmFile(sessionId: String): File = File(sessionDirectory(sessionId), RTCM_FILE_NAME)

    /** Normal disconnect calls finishSession first; close is process-shutdown best effort. */
    fun close() {
        commands.close()
    }

    private fun sessionDirectory(sessionId: String): File {
        require(SAFE_SESSION_ID.matches(sessionId)) { "Invalid session id" }
        return File(rootDirectory, sessionId)
    }

    private data class ActiveSession(
        val id: String,
        val nmea: FileOutputStream,
        val rtcm: FileOutputStream,
    ) {
        fun close() {
            runCatching { nmea.flush() }
            runCatching { rtcm.flush() }
            runCatching { nmea.close() }
            runCatching { rtcm.close() }
        }
    }

    private enum class Stream { NMEA, RTCM }

    private sealed interface Command {
        data class Start(val sessionId: String, val result: CompletableDeferred<Result<Unit>>) : Command
        data class Write(val stream: Stream, val bytes: ByteArray) : Command
        data class Finish(val result: CompletableDeferred<Result<Unit>>) : Command
        data class Delete(val sessionId: String, val result: CompletableDeferred<Result<Unit>>) : Command
    }

    companion object {
        const val NMEA_FILE_NAME = "nmea.log"
        const val RTCM_FILE_NAME = "rtcm.log"
        private val SAFE_SESSION_ID = Regex("[A-Za-z0-9_-]+")

        private fun deleteRecursively(file: File) {
            if (file.isDirectory) file.listFiles().orEmpty().forEach(::deleteRecursively)
            if (file.exists() && !file.delete()) error("Unable to delete ${file.name}")
        }
    }
}
