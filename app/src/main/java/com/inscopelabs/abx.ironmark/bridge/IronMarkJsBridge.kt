package com.inscopelabs.abx.ironmark.bridge

import android.net.Uri
import android.webkit.JavascriptInterface
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.inscopelabs.abx.ironmark.repository.FileManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class IronMarkJsBridge(
    private val fileManager: FileManager,
    private val scope: CoroutineScope,
    private val onProgress: (Float) -> Unit,
    private val onLog: (String) -> Unit
) {
    private val gson = Gson()

    @JavascriptInterface
    fun getFileSize(fileUri: String): Long {
        return try {
            val uri = Uri.parse(fileUri)
            fileManager.getFileSizeFromUri(uri)
        } catch (e: Exception) {
            onLog("Bridge error in getFileSize: ${e.message}")
            0L
        }
    }

    @JavascriptInterface
    fun readChunk(fileUri: String, offset: Long, length: Int): String {
        return try {
            val uri = Uri.parse(fileUri)
            runBlocking(Dispatchers.IO) {
                fileManager.readChunkBase64(uri, offset, length)
            }
        } catch (e: Exception) {
            onLog("Bridge error in readChunk: ${e.message}")
            ""
        }
    }

    @JavascriptInterface
    fun writeChunk(partName: String, base64Data: String): String {
        return try {
            runBlocking(Dispatchers.IO) {
                fileManager.writeChunkFromBase64(partName, base64Data)
            }
        } catch (e: Exception) {
            onLog("Bridge error in writeChunk: ${e.message}")
            ""
        }
    }

    @JavascriptInterface
    fun writeFile(filename: String, content: String): String {
        return try {
            runBlocking(Dispatchers.IO) {
                fileManager.writeTextFile(filename, content)
            }
        } catch (e: Exception) {
            onLog("Bridge error in writeFile: ${e.message}")
            ""
        }
    }

    @JavascriptInterface
    fun reportProgress(percent: Double) {
        scope.launch(Dispatchers.Main) {
            onProgress(percent.toFloat().coerceIn(0f, 100f))
        }
    }

    @JavascriptInterface
    fun log(message: String) {
        scope.launch(Dispatchers.Main) {
            onLog(message)
        }
    }

    @JavascriptInterface
    fun runScript(script: String, payload: String): String {
        onLog("Bridge executing runScript payload length=${payload.length}")
        return try {
            val resultObj = JsonObject()
            resultObj.addProperty("status", "executed")
            resultObj.addProperty("receivedPayloadLength", payload.length)
            gson.toJson(resultObj)
        } catch (e: Exception) {
            onLog("Bridge runScript error: ${e.message}")
            "{\"error\":\"${e.message}\"}"
        }
    }

    @JavascriptInterface
    fun executeScript(script: String, params: String): String {
        return runScript(script, params)
    }
}
