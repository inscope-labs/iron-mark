package com.inscopelabs.abx.ironmark.model

import android.net.Uri

data class SelectedFileInfo(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String? = null
) {
    val formattedSize: String
        get() = formatBytes(sizeBytes)
}

data class PartFileInfo(
    val filename: String,
    val filePath: String,
    val partIndex: Int,
    val sizeBytes: Long,
    val createdAtMs: Long = System.currentTimeMillis()
) {
    val formattedSize: String
        get() = formatBytes(sizeBytes)
}

data class ScriptTemplate(
    val id: String,
    val name: String,
    val description: String,
    val code: String,
    val defaultParamsJson: String = "{}"
)

enum class LogLevel {
    INFO, SUCCESS, WARNING, ERROR, PROGRESS, JS
}

data class LogEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestampMs: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val message: String
)

sealed class ExecutionState {
    object Idle : ExecutionState()
    data class Running(val progressPercent: Float, val statusMessage: String) : ExecutionState()
    data class Success(
        val resultJson: String,
        val partsCreated: List<PartFileInfo>,
        val durationMs: Long,
        val totalBytesProcessed: Long
    ) : ExecutionState()
    data class Error(val errorMessage: String, val stackTrace: String? = null) : ExecutionState()
}

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1]
    return String.format("%.2f %cB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
}
