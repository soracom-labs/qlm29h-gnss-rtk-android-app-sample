package jp.co.soracom.qlm29hrtk.sessionlog

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider

object SessionLogShare {
    fun share(context: Context, export: SessionLogExport) {
        val uris = ArrayList(export.files.map { file ->
            FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        })
        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris.single())
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        }.apply {
            type = if (uris.size == 1) "text/plain" else "application/octet-stream"
            clipData = ClipData.newUri(context.contentResolver, "QGNSS session logs", uris.first()).also { clip ->
                uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share historical log"))
    }
}
