package com.inscopelabs.abx.ironmark.bridge

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.inscopelabs.abx.ironmark.repository.FileManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class WebViewEngine(
    private val context: Context,
    private val fileManager: FileManager,
    private val scope: CoroutineScope,
    private val onProgress: (Float) -> Unit,
    private val onLog: (String) -> Unit
) {
    private var webView: WebView? = null
    private var isReady = false
    private val bridge = IronMarkJsBridge(fileManager, scope, onProgress, onLog)

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    fun initWebView() {
        if (webView != null) return

        webView = WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                allowFileAccess = false
                allowContentAccess = true
                domStorageEnabled = false
                cacheMode = WebSettings.LOAD_NO_CACHE
            }

            addJavascriptInterface(bridge, "Android")

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    isReady = true
                    onLog("IronMark JS Engine initialized and ready.")
                }
            }

            val htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <title>IronMark JS Bridge Runtime</title>
                </head>
                <body>
                    <script>
                        window.progressUpdate = function(percent) {
                            if (window.Android && window.Android.reportProgress) {
                                window.Android.reportProgress(percent);
                            }
                        };
                        window.logMessage = function(msg) {
                            if (window.Android && window.Android.log) {
                                window.Android.log(msg);
                            }
                        };
                        console.log = function(...args) {
                            window.logMessage(args.map(a => typeof a === 'object' ? JSON.stringify(a) : a).join(' '));
                        };
                        console.error = function(...args) {
                            window.logMessage("ERROR: " + args.join(' '));
                        };
                        window.onerror = function(msg, url, line) {
                            window.logMessage("JS Exec Error: " + msg + " at line " + line);
                        };
                    </script>
                </body>
                </html>
            """.trimIndent()

            loadDataWithBaseURL("https://ironmark.internal/", htmlContent, "text/html", "UTF-8", null)
        }
    }

    suspend fun executeScript(jsCode: String): String = suspendCancellableCoroutine { continuation ->
        scope.launch(Dispatchers.Main) {
            val wv = webView
            if (wv == null) {
                continuation.resume("{\"error\":\"WebView engine not initialized\"}")
                return@launch
            }

            wv.evaluateJavascript(jsCode) { result ->
                continuation.resume(result ?: "null")
            }
        }
    }

    fun destroy() {
        webView?.apply {
            stopLoading()
            clearHistory()
            removeAllViews()
            destroy()
        }
        webView = null
        isReady = false
    }
}
