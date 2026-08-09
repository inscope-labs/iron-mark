package com.inscopelabs.abx.ironmark.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.ironmark.model.ExecutionState
import com.inscopelabs.abx.ironmark.ui.components.FileCard
import com.inscopelabs.abx.ironmark.ui.components.MetricCard
import com.inscopelabs.abx.ironmark.ui.components.PartFileItemView
import com.inscopelabs.abx.ironmark.ui.components.StatusBadge
import com.inscopelabs.abx.ironmark.ui.theme.AmberSecondary
import com.inscopelabs.abx.ironmark.ui.theme.CyanPrimary
import com.inscopelabs.abx.ironmark.ui.theme.DarkBorder
import com.inscopelabs.abx.ironmark.ui.theme.DarkSurface
import com.inscopelabs.abx.ironmark.ui.theme.DarkSurfaceVariant
import com.inscopelabs.abx.ironmark.ui.theme.EmeraldSuccess
import com.inscopelabs.abx.ironmark.ui.theme.RoseError
import com.inscopelabs.abx.ironmark.ui.theme.TextMuted
import com.inscopelabs.abx.ironmark.ui.theme.TextPrimary
import com.inscopelabs.abx.ironmark.ui.theme.TextSecondary
import com.inscopelabs.abx.ironmark.viewmodel.IronMarkViewModel

@Composable
fun TextSplitterScreen(
    viewModel: IronMarkViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedFile by viewModel.selectedFile.collectAsState()
    val executionState by viewModel.executionState.collectAsState()
    val outputParts by viewModel.outputParts.collectAsState()

    var selectedStrategy by remember { mutableStateOf("size") }
    var maxChunkSizeKb by remember { mutableFloatStateOf(18f) }
    var addMarkers by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.selectScriptTemplate(IronMarkViewModel.CHAT_RESPONSE_SPLITTER_SCRIPT)
    }

    LaunchedEffect(selectedStrategy, maxChunkSizeKb, addMarkers) {
        val maxBytes = maxChunkSizeKb.toInt() * 1024
        val json = "{\n  \"strategy\": \"$selectedStrategy\",\n  \"maxChunkBytes\": $maxBytes,\n  \"addMarkers\": $addMarkers\n}"
        viewModel.setCustomJsonParams(json)
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.onFileSelected(uri)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Text Splitter",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "Split AI agent responses & long text at natural boundaries",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                StatusBadge(state = executionState)
            }
        }

        item {
            FileCard(
                fileInfo = selectedFile,
                onSelectFileClick = { filePickerLauncher.launch("text/*") }
            )
        }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = DarkSurface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SPLITTING STRATEGY",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyanPrimary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val strategies = listOf(
                        "size" to "By size",
                        "section" to "By section",
                        "code-isolated" to "Code isolated"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        strategies.forEach { (key, label) ->
                            val isSelected = selectedStrategy == key
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedStrategy = key }
                                    .testTag("strategy_$key"),
                                color = if (isSelected) CyanPrimary.copy(alpha = 0.2f) else DarkSurfaceVariant,
                                border = BorderStroke(1.dp, if (isSelected) CyanPrimary else DarkBorder)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) CyanPrimary else TextSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = DarkSurface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "CHUNK CONFIGURATION",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyanPrimary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Max chunk size: ${maxChunkSizeKb.toInt()} KB",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = maxChunkSizeKb,
                        onValueChange = { maxChunkSizeKb = it },
                        valueRange = 4f..19f,
                        steps = 14,
                        colors = SliderDefaults.colors(
                            thumbColor = CyanPrimary,
                            activeTrackColor = CyanPrimary,
                            inactiveTrackColor = DarkSurfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("max_chunk_size_slider")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Prefix each part with [Part X/Y]",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Adds part index markers at beginning of each chunk",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = addMarkers,
                            onCheckedChange = { addMarkers = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CyanPrimary,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkSurfaceVariant
                            ),
                            modifier = Modifier.testTag("add_markers_switch")
                        )
                    }
                }
            }
        }

        item {
            val isRunning = executionState is ExecutionState.Running
            Button(
                onClick = { viewModel.executeCustomScript() },
                enabled = selectedFile != null && !isRunning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("execute_text_splitter_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanPrimary,
                    contentColor = Color.White,
                    disabledContainerColor = DarkSurfaceVariant,
                    disabledContentColor = TextMuted
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CallSplit,
                    contentDescription = "Split",
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isRunning) "Splitting Text via JS Engine..." else "Execute Text Splitter",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (executionState is ExecutionState.Running) {
            val runningState = executionState as ExecutionState.Running
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyanPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    color = DarkSurface
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PROGRESS",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyanPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${runningState.progressPercent.toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                color = CyanPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { runningState.progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = CyanPrimary,
                            trackColor = DarkSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = runningState.statusMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        if (executionState is ExecutionState.Success) {
            val successState = executionState as ExecutionState.Success
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, EmeraldSuccess.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    color = DarkSurface
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Text Split Successfully!",
                                style = MaterialTheme.typography.titleMedium,
                                color = EmeraldSuccess,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricCard(
                                title = "Parts Created",
                                value = "${successState.partsCreated.size}",
                                icon = Icons.Default.FolderZip,
                                modifier = Modifier.weight(1f),
                                accentColor = CyanPrimary
                            )
                            MetricCard(
                                title = "Duration",
                                value = "${successState.durationMs} ms",
                                icon = Icons.Default.Timer,
                                modifier = Modifier.weight(1f),
                                accentColor = AmberSecondary
                            )
                        }
                    }
                }
            }
        }

        if (executionState is ExecutionState.Error) {
            val errorState = executionState as ExecutionState.Error
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, RoseError.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    color = DarkSurface
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ERROR",
                            style = MaterialTheme.typography.labelMedium,
                            color = RoseError,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = errorState.errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        if (outputParts.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GENERATED PARTS (${outputParts.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    TextButton(
                        onClick = {
                            // TODO: Navigate to Outputs tab if cross-tab callback is added
                        }
                    ) {
                        Text(
                            text = "View all in Outputs",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyanPrimary
                        )
                    }
                }
            }

            items(outputParts) { part ->
                PartFileItemView(
                    part = part,
                    onDelete = { path -> viewModel.deleteOutputPart(path) },
                    onShare = { path ->
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "IronMark Text Part: ${part.filename}")
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Part File"))
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
