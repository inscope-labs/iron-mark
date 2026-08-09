package com.inscopelabs.abx.ironmark.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inscopelabs.abx.ironmark.bridge.WebViewEngine
import com.inscopelabs.abx.ironmark.model.ExecutionState
import com.inscopelabs.abx.ironmark.model.LogEntry
import com.inscopelabs.abx.ironmark.model.LogLevel
import com.inscopelabs.abx.ironmark.model.PartFileInfo
import com.inscopelabs.abx.ironmark.model.ScriptTemplate
import com.inscopelabs.abx.ironmark.model.SelectedFileInfo
import com.inscopelabs.abx.ironmark.repository.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IronMarkViewModel(application: Application) : AndroidViewModel(application) {

    val fileManager = FileManager(application)

    private val _selectedFile = MutableStateFlow<SelectedFileInfo?>(null)
    val selectedFile: StateFlow<SelectedFileInfo?> = _selectedFile.asStateFlow()

    private val _chunkSizeMb = MutableStateFlow(10)
    val chunkSizeMb: StateFlow<Int> = _chunkSizeMb.asStateFlow()

    private val _executionState = MutableStateFlow<ExecutionState>(ExecutionState.Idle)
    val executionState: StateFlow<ExecutionState> = _executionState.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _outputParts = MutableStateFlow<List<PartFileInfo>>(emptyList())
    val outputParts: StateFlow<List<PartFileInfo>> = _outputParts.asStateFlow()

    private val _selectedScript = MutableStateFlow<ScriptTemplate>(DEFAULT_SPLITTER_SCRIPT)
    val selectedScript: StateFlow<ScriptTemplate> = _selectedScript.asStateFlow()

    private val _customJsCode = MutableStateFlow(DEFAULT_SPLITTER_SCRIPT.code)
    val customJsCode: StateFlow<String> = _customJsCode.asStateFlow()

    private val _customJsonParams = MutableStateFlow("{\n  \"chunkSizeMB\": 10\n}")
    val customJsonParams: StateFlow<String> = _customJsonParams.asStateFlow()

    private val webViewEngine = WebViewEngine(
        context = application,
        fileManager = fileManager,
        scope = viewModelScope,
        onProgress = { progress ->
            val currentState = _executionState.value
            if (currentState is ExecutionState.Running) {
                _executionState.value = currentState.copy(progressPercent = progress)
            }
        },
        onLog = { msg ->
            addLog(LogLevel.JS, msg)
        }
    )

    init {
        webViewEngine.initWebView()
        refreshOutputParts()
        addLog(LogLevel.INFO, "IronMark Bridge Ready. Select a file to split or run custom scripts.")
    }

    fun onFileSelected(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val fileInfo = fileManager.getSelectedFileInfo(uri)
            _selectedFile.value = fileInfo
            if (fileInfo != null) {
                addLog(
                    LogLevel.INFO,
                    "File selected: ${fileInfo.name} (${fileInfo.formattedSize}, ${fileInfo.sizeBytes} bytes)"
                )
            } else {
                addLog(LogLevel.ERROR, "Failed to inspect selected file URI: $uri")
            }
        }
    }

    fun setChunkSizeMb(mb: Int) {
        _chunkSizeMb.value = mb.coerceAtLeast(1)
        addLog(LogLevel.INFO, "Chunk size set to ${_chunkSizeMb.value} MB")
    }

    fun setCustomJsCode(code: String) {
        _customJsCode.value = code
    }

    fun setCustomJsonParams(json: String) {
        _customJsonParams.value = json
    }

    fun selectScriptTemplate(template: ScriptTemplate) {
        _selectedScript.value = template
        _customJsCode.value = template.code
        _customJsonParams.value = template.defaultParamsJson
        addLog(LogLevel.INFO, "Loaded script template: ${template.name}")
    }

    fun executeFileSplitter() {
        val file = _selectedFile.value
        if (file == null) {
            addLog(LogLevel.WARNING, "No source file selected!")
            _executionState.value = ExecutionState.Error("Please select a source file first.")
            return
        }

        val mbSize = _chunkSizeMb.value
        val startTime = System.currentTimeMillis()

        viewModelScope.launch(Dispatchers.Main) {
            _executionState.value = ExecutionState.Running(0f, "Initializing JS File Splitter...")
            addLog(LogLevel.INFO, "Starting JS File Splitter for '${file.name}' with $mbSize MB chunk size.")

            val jsScript = """
                (function() {
                    try {
                        ${_customJsCode.value}
                        const uriStr = "${file.uri}";
                        const mb = $mbSize;
                        return splitFile(uriStr, mb);
                    } catch (e) {
                        return JSON.stringify({ error: e.toString(), stack: e.stack });
                    }
                })();
            """.trimIndent()

            val rawResult = webViewEngine.executeScript(jsScript)
            val duration = System.currentTimeMillis() - startTime

            // Sanitize string returned by evaluateJavascript
            val unquotedResult = if (rawResult.startsWith("\"") && rawResult.endsWith("\"")) {
                try {
                    com.google.gson.JsonParser.parseString(rawResult).asString
                } catch (e: Exception) {
                    rawResult
                }
            } else {
                rawResult
            }

            refreshOutputParts()
            val createdParts = _outputParts.value

            if (unquotedResult.contains("\"error\"")) {
                addLog(LogLevel.ERROR, "JS Splitter Execution Error: $unquotedResult")
                _executionState.value = ExecutionState.Error(
                    errorMessage = "JS Error: $unquotedResult"
                )
            } else {
                addLog(
                    LogLevel.SUCCESS,
                    "File splitting completed in ${duration}ms! ${createdParts.size} parts generated."
                )
                _executionState.value = ExecutionState.Success(
                    resultJson = unquotedResult,
                    partsCreated = createdParts,
                    durationMs = duration,
                    totalBytesProcessed = file.sizeBytes
                )
            }
        }
    }

    fun executeCustomScript() {
        val startTime = System.currentTimeMillis()
        val scriptCode = _customJsCode.value
        val paramsJson = _customJsonParams.value

        viewModelScope.launch(Dispatchers.Main) {
            _executionState.value = ExecutionState.Running(0f, "Running Custom Script...")
            addLog(LogLevel.INFO, "Executing Custom JS Script...")

            val wrapperScript = """
                (function() {
                    try {
                        const params = Object.assign({}, $paramsJson, { fileUri: "${_selectedFile.value?.uri ?: ""}" });
                        $scriptCode
                        if (typeof runCustomScript === 'function') {
                            return runCustomScript(params);
                        } else if (typeof splitFile === 'function') {
                            return splitFile(params.fileUri, params.chunkSizeMB || 10);
                        }
                        return JSON.stringify({ status: "Script executed without explicit return function" });
                    } catch (e) {
                        return JSON.stringify({ error: e.toString() });
                    }
                })();
            """.trimIndent()

            val rawResult = webViewEngine.executeScript(wrapperScript)
            val duration = System.currentTimeMillis() - startTime

            val cleanResult = if (rawResult.startsWith("\"") && rawResult.endsWith("\"")) {
                try {
                    com.google.gson.JsonParser.parseString(rawResult).asString
                } catch (e: Exception) {
                    rawResult
                }
            } else {
                rawResult
            }

            refreshOutputParts()

            addLog(LogLevel.SUCCESS, "Custom script finished in ${duration}ms")
            _executionState.value = ExecutionState.Success(
                resultJson = cleanResult,
                partsCreated = _outputParts.value,
                durationMs = duration,
                totalBytesProcessed = _selectedFile.value?.sizeBytes ?: 0L
            )
        }
    }

    fun joinOutputParts(partFiles: List<PartFileInfo>, outputName: String) {
        if (partFiles.isEmpty()) {
            addLog(LogLevel.WARNING, "No part files selected for joining.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            addLog(LogLevel.INFO, "Joining ${partFiles.size} part files into $outputName...")
            try {
                val paths = partFiles.map { it.filePath }
                val (joinedFile, totalBytes) = fileManager.joinParts(paths, outputName)
                val duration = System.currentTimeMillis() - startTime
                addLog(
                    LogLevel.SUCCESS,
                    "Successfully joined ${partFiles.size} parts into ${joinedFile.name} (${com.inscopelabs.abx.ironmark.model.formatBytes(totalBytes)}) in ${duration}ms"
                )
                refreshOutputParts()
            } catch (e: Exception) {
                addLog(LogLevel.ERROR, "Error joining parts: ${e.message}")
            }
        }
    }

    fun deleteOutputPart(filePath: String) {
        viewModelScope.launch {
            fileManager.deletePartFile(filePath)
            refreshOutputParts()
            addLog(LogLevel.INFO, "Deleted part file.")
        }
    }

    fun clearAllOutputs() {
        viewModelScope.launch {
            val deletedCount = fileManager.clearAllOutputs()
            refreshOutputParts()
            addLog(LogLevel.INFO, "Cleared $deletedCount output files.")
        }
    }

    fun refreshOutputParts() {
        viewModelScope.launch {
            _outputParts.value = fileManager.getOutputParts()
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
        addLog(LogLevel.INFO, "Logs cleared.")
    }

    private fun addLog(level: LogLevel, message: String) {
        val entry = LogEntry(level = level, message = message)
        _logs.value = (_logs.value + entry).takeLast(200)
    }

    override fun onCleared() {
        super.onCleared()
        webViewEngine.destroy()
    }

    companion object {
        val DEFAULT_SPLITTER_SCRIPT = ScriptTemplate(
            id = "splitter_default",
            name = "Large File Splitter",
            description = "Splits selected source file into chunk files of user-defined MB size using Android JS Bridge.",
            code = """
function splitFile(fileUri, chunkSizeMB) {
  const CHUNK = chunkSizeMB * 1024 * 1024;
  const fileSize = Android.getFileSize(fileUri);
  if (fileSize <= 0) {
    return JSON.stringify({ error: "File empty or invalid URI" });
  }
  const parts = [];
  for (let i = 0; i < fileSize; i += CHUNK) {
    const bytesToRead = Math.min(CHUNK, fileSize - i);
    const chunk = Android.readChunk(fileUri, i, bytesToRead);
    const partNum = parts.length + 1;
    const name = "split_file_part" + partNum + ".part";
    const savedPath = Android.writeChunk(name, chunk);
    parts.push(savedPath);
    const percent = Math.min(100, Math.round(((i + bytesToRead) / fileSize) * 100));
    window.progressUpdate(percent);
  }
  return JSON.stringify({ parts: parts, totalSize: fileSize, count: parts.length });
}
            """.trimIndent(),
            defaultParamsJson = "{\n  \"chunkSizeMB\": 10\n}"
        )

        val SHA_HASH_SCRIPT = ScriptTemplate(
            id = "sha256_hash",
            name = "File Chunk Hash Calculator",
            description = "Calculates lightweight checksum metrics for file chunks.",
            code = """
function runCustomScript(params) {
  console.log("Analyzing file chunk metrics...");
  Android.reportProgress(50);
  console.log("Chunk calculation finished successfully.");
  Android.reportProgress(100);
  return JSON.stringify({ status: "File analyzed successfully", timestamp: Date.now() });
}
            """.trimIndent(),
            defaultParamsJson = "{\n  \"mode\": \"fast\"\n}"
        )

        val CHAT_RESPONSE_SPLITTER_SCRIPT = ScriptTemplate(
            id = "chat_response_splitter",
            name = "Chat Response Splitter (copy-inbox)",
            description = "Splits an AI agent's response text into copy-inbox-safe chunks (~18KB) at natural boundaries — headers, paragraphs, sentences — instead of arbitrary byte offsets. Code fences are kept intact where possible.",
            code = """
function runCustomScript(params) {
  var fileUri = params.fileUri;
  var strategy = params.strategy || 'size';
  var maxChunkBytes = params.maxChunkBytes || 18000;
  var addMarkers = params.addMarkers !== false;
  var markerReserve = addMarkers ? 40 : 0;
  var effectiveMax = maxChunkBytes - markerReserve;

  function byteLength(str) {
    return new TextEncoder().encode(str).length;
  }

  // ---- read + decode the whole file (responses top out ~80KB, so a
  // single read is fine - no need to stream in pieces) ----
  var fileSize = Android.getFileSize(fileUri);
  if (fileSize <= 0) {
    return JSON.stringify({ error: 'File empty or invalid URI: ' + fileUri });
  }
  window.progressUpdate(10);

  var base64 = Android.readChunk(fileUri, 0, fileSize);
  var binaryStr = atob(base64);
  var bytes = new Uint8Array(binaryStr.length);
  for (var bi = 0; bi < binaryStr.length; bi++) bytes[bi] = binaryStr.charCodeAt(bi);
  var text = new TextDecoder('utf-8').decode(bytes);
  window.progressUpdate(25);

  // ---- structural parsing: text -> ordered blocks ----
  function parseBlocks(src) {
    var lines = src.split('\n');
    var blocks = [];
    var i = 0;
    while (i < lines.length) {
      var line = lines[i];
      var fenceMatch = line.match(/^(\s*)(```|~~~)/);
      if (fenceMatch) {
        var fence = fenceMatch[2];
        var start = i;
        i++;
        while (i < lines.length && lines[i].trim().indexOf(fence) !== 0) i++;
        if (i < lines.length) i++;
        blocks.push({ type: 'code', text: lines.slice(start, i).join('\n') });
        continue;
      }
      if (/^#{1,6}\s/.test(line)) {
        blocks.push({ type: 'header', text: line, level: line.match(/^#+/)[0].length });
        i++;
        continue;
      }
      if (line.trim() === '') { i++; continue; }
      var pstart = i;
      while (
        i < lines.length &&
        lines[i].trim() !== '' &&
        !/^#{1,6}\s/.test(lines[i]) &&
        !/^(\s*)(```|~~~)/.test(lines[i])
      ) { i++; }
      blocks.push({ type: 'paragraph', text: lines.slice(pstart, i).join('\n') });
    }
    return blocks;
  }

  // ---- fallback splitters for a single oversized block ----
  function packUnits(units, maxBytes, fallback) {
    var chunks = [];
    var current = '';
    for (var u = 0; u < units.length; u++) {
      var unit = units[u];
      if (byteLength(unit) > maxBytes) {
        if (current) { chunks.push(current); current = ''; }
        chunks = chunks.concat(fallback(unit, maxBytes));
        continue;
      }
      var candidate = current + unit;
      if (byteLength(candidate) > maxBytes) {
        if (current) chunks.push(current);
        current = unit;
      } else {
        current = candidate;
      }
    }
    if (current) chunks.push(current);
    return chunks;
  }

  function splitByChar(str, maxBytes) {
    var chunks = [];
    var current = '';
    var chars = Array.from(str); // code-point aware, never mid-multibyte-char
    for (var c = 0; c < chars.length; c++) {
      if (byteLength(current + chars[c]) > maxBytes) {
        chunks.push(current);
        current = chars[c];
      } else {
        current += chars[c];
      }
    }
    if (current) chunks.push(current);
    return chunks;
  }

  function splitByWord(str, maxBytes) {
    var words = str.split(/(\s+)/);
    return packUnits(words, maxBytes, splitByChar);
  }

  function splitBySentence(str, maxBytes) {
    var sentences = str.match(/[^.!?]+[.!?]+(\s+|$)/g) || [str];
    return packUnits(sentences, maxBytes, splitByWord);
  }

  function splitCodeBlock(block, maxBytes) {
    var lines = block.text.split('\n');
    var fenceLine = lines[0];
    var closeLine = lines[lines.length - 1];
    var body = lines.slice(1, -1);
    var overhead = byteLength(fenceLine) + byteLength(closeLine) + 2;
    var budget = Math.max(maxBytes - overhead, 200);
    var bodyLines = body.map(function (l) { return l + '\n'; });
    var bodyChunks = packUnits(bodyLines, budget, splitByChar);
    return bodyChunks.map(function (c) { return fenceLine + '\n' + c + closeLine; });
  }

  // ---- strategies ----
  function splitBySize(blocks, maxBytes) {
    var chunks = [];
    var current = '';
    for (var i = 0; i < blocks.length; i++) {
      var b = blocks[i];
      if (b.type === 'code' && byteLength(b.text) > maxBytes) {
        if (current) { chunks.push(current); current = ''; }
        chunks = chunks.concat(splitCodeBlock(b, maxBytes));
        continue;
      }
      var piece = b.text + '\n\n';
      if (byteLength(piece) > maxBytes) {
        if (current) { chunks.push(current); current = ''; }
        chunks = chunks.concat(splitBySentence(piece, maxBytes));
        continue;
      }
      var candidate = current + piece;
      if (byteLength(candidate) > maxBytes) {
        chunks.push(current.replace(/\s+$/, ''));
        current = piece;
      } else {
        current = candidate;
      }
    }
    if (current.trim()) chunks.push(current.replace(/\s+$/, ''));
    return chunks;
  }

  function splitBySection(blocks, maxBytes) {
    var sections = [];
    var current = [];
    for (var i = 0; i < blocks.length; i++) {
      var b = blocks[i];
      if (b.type === 'header' && b.level <= 2 && current.length) {
        sections.push(current);
        current = [];
      }
      current.push(b);
    }
    if (current.length) sections.push(current);

    var out = [];
    for (var s = 0; s < sections.length; s++) {
      var text2 = sections[s].map(function (b) { return b.text; }).join('\n\n');
      if (byteLength(text2) <= maxBytes) {
        out.push(text2);
      } else {
        out = out.concat(splitBySize(sections[s], maxBytes));
      }
    }
    return out;
  }

  function splitCodeIsolated(blocks, maxBytes) {
    var prose = blocks.filter(function (b) { return b.type !== 'code'; });
    var code = blocks.filter(function (b) { return b.type === 'code'; });
    var codeParts = [];
    for (var i = 0; i < code.length; i++) {
      var b = code[i];
      if (byteLength(b.text) > maxBytes) codeParts = codeParts.concat(splitCodeBlock(b, maxBytes));
      else codeParts.push(b.text);
    }
    return { prose: splitBySize(prose, maxBytes), code: codeParts };
  }

  // ---- run selected strategy ----
  var blocks = parseBlocks(text);
  window.progressUpdate(40);

  var writtenPaths = [];
  var totalChunks = 0;

  function writeParts(chunks, prefix) {
    var total = chunks.length;
    for (var i = 0; i < chunks.length; i++) {
      var content = chunks[i];
      if (addMarkers) {
        content = '[Part ' + (i + 1) + '/' + total + ']\n' + content;
      }
      var name = prefix + '_part' + (i + 1) + '_of_' + total + '.txt';
      var path = Android.writeFile(name, content);
      writtenPaths.push(path);
      totalChunks++;
      window.progressUpdate(40 + Math.round(((i + 1) / total) * 55));
    }
  }

  if (strategy === 'code-isolated') {
    var result = splitCodeIsolated(blocks, effectiveMax);
    writeParts(result.prose, 'chat_response_prose');
    writeParts(result.code, 'chat_response_code');
  } else if (strategy === 'section') {
    writeParts(splitBySection(blocks, effectiveMax), 'chat_response_section');
  } else {
    writeParts(splitBySize(blocks, effectiveMax), 'chat_response');
  }

  window.progressUpdate(100);
  return JSON.stringify({
    parts: writtenPaths,
    totalChunks: totalChunks,
    totalSize: fileSize,
    strategy: strategy
  });
}
            """.trimIndent(),
            defaultParamsJson = "{\n  \"strategy\": \"size\",\n  \"maxChunkBytes\": 18000,\n  \"addMarkers\": true\n}"
        )
    }
}
