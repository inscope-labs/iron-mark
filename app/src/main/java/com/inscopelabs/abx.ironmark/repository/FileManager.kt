package com.inscopelabs.abx.ironmark.repository

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Base64
import com.inscopelabs.abx.ironmark.model.PartFileInfo
import com.inscopelabs.abx.ironmark.model.SelectedFileInfo
import com.inscopelabs.abx.ironmark.model.formatBytes
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FileManager(private val context: Context) {

    private val outputDir: File
        get() {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "IronMark_Outputs")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    fun getSelectedFileInfo(uri: Uri): SelectedFileInfo? {
        var name = "unknown_file"
        var sizeBytes = 0L
        val mimeType = context.contentResolver.getType(uri)

        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                    if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) sizeBytes = cursor.getLong(sizeIndex)
                }
            }
        } else if (uri.scheme == "file") {
            val file = File(uri.path ?: "")
            if (file.exists()) {
                name = file.name
                sizeBytes = file.length()
            }
        }

        if (sizeBytes == 0L) {
            sizeBytes = getFileSizeFromUri(uri)
        }

        return SelectedFileInfo(
            uri = uri,
            name = name,
            sizeBytes = sizeBytes,
            mimeType = mimeType
        )
    }

    fun getFileSizeFromUri(uri: Uri): Long {
        return try {
            if (uri.scheme == "file") {
                File(uri.path ?: "").length()
            } else {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    pfd.statSize
                } ?: 0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    suspend fun readChunkBase64(uri: Uri, offset: Long, length: Int): String = withContext(Dispatchers.IO) {
        val buffer = ByteArray(length)
        var bytesRead = 0

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val skipped = inputStream.skip(offset)
            if (skipped < offset) {
                // If skip didn't skip all bytes, loop to consume
                var remainingToSkip = offset - skipped
                val tempBuf = ByteArray(8192)
                while (remainingToSkip > 0) {
                    val read = inputStream.read(tempBuf, 0, Math.min(tempBuf.size.toLong(), remainingToSkip).toInt())
                    if (read <= 0) break
                    remainingToSkip -= read
                }
            }

            var totalRead = 0
            while (totalRead < length) {
                val read = inputStream.read(buffer, totalRead, length - totalRead)
                if (read <= 0) break
                totalRead += read
            }
            bytesRead = totalRead
        }

        val finalBuffer = if (bytesRead == length) buffer else buffer.copyOf(bytesRead)
        Base64.encodeToString(finalBuffer, Base64.NO_WRAP)
    }

    suspend fun writeChunkFromBase64(partName: String, base64Data: String): String = withContext(Dispatchers.IO) {
        val bytes = Base64.decode(base64Data, Base64.DEFAULT)
        val file = File(outputDir, partName)
        FileOutputStream(file, false).use { fos ->
            fos.write(bytes)
            fos.flush()
        }
        file.absolutePath
    }

    suspend fun writeTextFile(filename: String, textContent: String): String = withContext(Dispatchers.IO) {
        val file = File(outputDir, filename)
        file.writeText(textContent)
        file.absolutePath
    }

    suspend fun getOutputParts(): List<PartFileInfo> = withContext(Dispatchers.IO) {
        val files = outputDir.listFiles() ?: emptyArray()
        files.mapIndexed { index, file ->
            PartFileInfo(
                filename = file.name,
                filePath = file.absolutePath,
                partIndex = index + 1,
                sizeBytes = file.length(),
                createdAtMs = file.lastModified()
            )
        }.sortedByDescending { it.createdAtMs }
    }

    suspend fun joinParts(partPaths: List<String>, outputFilename: String): Pair<File, Long> = withContext(Dispatchers.IO) {
        val outputFile = File(outputDir, outputFilename)
        var totalBytesWritten = 0L

        FileOutputStream(outputFile, false).use { fos ->
            for (path in partPaths) {
                val partFile = File(path)
                if (partFile.exists()) {
                    FileInputStream(partFile).use { fis ->
                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        while (fis.read(buffer).also { read = it } != -1) {
                            fos.write(buffer, 0, read)
                            totalBytesWritten += read
                        }
                    }
                }
            }
            fos.flush()
        }
        Pair(outputFile, totalBytesWritten)
    }

    suspend fun deletePartFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (file.exists()) file.delete() else false
    }

    suspend fun clearAllOutputs(): Int = withContext(Dispatchers.IO) {
        val files = outputDir.listFiles() ?: emptyArray()
        var count = 0
        for (f in files) {
            if (f.delete()) count++
        }
        count
    }
}
