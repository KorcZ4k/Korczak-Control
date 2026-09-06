package com.korczak.control.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object AppInstaller {
    suspend fun downloadAndInstall(context: Context, update: AppUpdate, onProgress: (Int) -> Unit) = withContext(Dispatchers.IO) {
        val updatesDirectory = File(context.cacheDir, "updates").apply { mkdirs() }
        val apkFile = File(updatesDirectory, "korczak-control-${update.version}.apk")
        val connection = (URL(update.downloadUrl).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 15000
            readTimeout = 30000
            requestMethod = "GET"
        }
        try {
            if (connection.responseCode !in 200..299) throw IllegalStateException("Não foi possível baixar a atualização.")
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                apkFile.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var downloaded = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        downloaded += count
                        if (total > 0) onProgress(((downloaded * 100) / total).toInt().coerceIn(0, 100))
                    }
                }
            }
            if (!apkFile.exists() || apkFile.length() == 0L) throw IllegalStateException("O arquivo da atualização está vazio.")
            install(context, apkFile)
        } finally {
            connection.disconnect()
        }
    }

    private fun install(context: Context, apkFile: File) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
