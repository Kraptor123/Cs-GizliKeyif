package com.kraptor

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

class ImageDownloader(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun sanitize(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
    }

    suspend fun downloadToDownloads(
        imageUrl: String,
        subDir: String? = null,
        fileName: String? = null
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val response = httpClient.newCall(
                Request.Builder()
                    .url(imageUrl)
                    .header("User-Agent", USER_AGENT)
                    .header("Referer", "https://simpcity.cr/")
                    .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                    .build()
            ).execute()

            val responseBody = response.body ?: return@withContext null

            val mimeType = responseBody.contentType()?.toString()?.substringBefore(";") ?: "image/jpeg"
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
            val finalFileName = fileName ?: "simpcity_${System.currentTimeMillis()}.$extension"
            val finalSubDir = if (subDir != null) sanitize(subDir) else null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveViaMediaStore(responseBody.byteStream(), finalSubDir, finalFileName, mimeType)
            } else {
                saveViaFileSystem(responseBody.byteStream(), finalSubDir, finalFileName)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun saveViaMediaStore(
        inputStream: java.io.InputStream,
        subDir: String?,
        fileName: String,
        mimeType: String
    ): Uri? {
        val relativePath = if (subDir != null) "Download/SimpCity/$subDir" else "Download/SimpCity"
        
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.IS_PENDING, 1)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
            }
        }

        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = context.contentResolver.insert(collection, contentValues) ?: return null

        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                inputStream.copyTo(outputStream, 8192)
            }

            contentValues.clear()
            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
            context.contentResolver.update(uri, contentValues, null, null)
            return uri
        } catch (e: Exception) {
            try { context.contentResolver.delete(uri, null, null) } catch (_: Exception) {}
            return null
        }
    }

    @Suppress("DEPRECATION")
    private fun saveViaFileSystem(
        inputStream: java.io.InputStream,
        subDir: String?,
        fileName: String
    ): Uri? {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val targetDir = if (subDir != null) File(downloadsDir, "SimpCity/$subDir") else File(downloadsDir, "SimpCity")
        targetDir.mkdirs()
        
        val targetFile = File(targetDir, fileName)
        targetFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream, 8192)
        }

        MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), null, null)
        return Uri.fromFile(targetFile)
    }

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"
    }
}
