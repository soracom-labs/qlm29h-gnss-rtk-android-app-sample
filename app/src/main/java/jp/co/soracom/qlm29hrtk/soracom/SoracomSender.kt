package jp.co.soracom.qlm29hrtk.soracom

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

data class SendResult(val successful: Boolean, val httpStatus: Int?, val message: String)

class SoracomSender(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build(),
    private val endpoint: String = ENDPOINT,
) {
    suspend fun send(payload: SoracomPayload): SendResult = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(endpoint)
            .post(PayloadBuilder.encode(payload).toRequestBody(JSON_MEDIA_TYPE))
            .build()
        try {
            client.newCall(request).execute().use { response ->
                SendResult(response.isSuccessful, response.code, "HTTP ${response.code}")
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            SendResult(false, null, error.message ?: error.javaClass.simpleName)
        }
    }

    companion object {
        const val ENDPOINT = "http://uni.soracom.io"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
