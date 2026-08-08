package com.inscopelabs.abx.ironmark.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.ironmark.model.ExecutionState
import com.inscopelabs.abx.ironmark.model.LogEntry
import com.inscopelabs.abx.ironmark.model.LogLevel
import com.inscopelabs.abx.ironmark.model.PartFileInfo
import com.inscopelabs.abx.ironmark.model.SelectedFileInfo
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accentColor: Color = CyanPrimary
) {
    Surface(
        modifier = modifier
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StatusBadge(state: ExecutionState, modifier: Modifier = Modifier) {
    val (color, text, icon) = when (state) {
        is ExecutionState.Idle -> Triple(TextMuted, "IDLE", Icons.Default.Info)
        is ExecutionState.Running -> Triple(CyanPrimary, "SPLITTING (${state.progressPercent.toInt()}%)", Icons.Default.PlayArrow)
        is ExecutionState.Success -> Triple(EmeraldSuccess, "COMPLETED", Icons.Default.CheckCircle)
        is ExecutionState.Error -> Triple(RoseError, "ERROR", Icons.Default.Error)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun FileCard(
    fileInfo: SelectedFileInfo?,
    onSelectFileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = DarkSurface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "SOURCE FILE",
                style = MaterialTheme.typography.labelMedium,
                color = CyanPrimary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (fileInfo != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyanPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.InsertDriveFile,
                            contentDescription = "File",
                            tint = CyanPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fileInfo.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 1
                        )
                        Text(
                            text = "${fileInfo.formattedSize} • ${fileInfo.mimeType ?: "binary/data"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelectFileClick() }
                            .testTag("change_file_button"),
                        color = DarkSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Text(
                            text = "Change",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = CyanPrimary
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceVariant)
                        .clickable { onSelectFileClick() }
                        .padding(20.dp)
                        .testTag("select_file_button"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "Select File",
                        tint = CyanPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Tap to Pick Source File",
                        style = MaterialTheme.typography.titleMedium,
                        color = CyanPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun PartFileItemView(
    part: PartFileInfo,
    onDelete: (String) -> Unit,
    onShare: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(part.createdAtMs))

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AmberSecondary.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, AmberSecondary.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "PART ${part.partIndex}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = AmberSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = part.filename,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = "${part.formattedSize} • Created at $timeStr",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            IconButton(
                onClick = { onShare(part.filePath) },
                modifier = Modifier.testTag("share_part_${part.partIndex}")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = { onDelete(part.filePath) },
                modifier = Modifier.testTag("delete_part_${part.partIndex}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = RoseError,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun LogItemView(entry: LogEntry, modifier: Modifier = Modifier) {
    val color = when (entry.level) {
        LogLevel.INFO -> CyanPrimary
        LogLevel.SUCCESS -> EmeraldSuccess
        LogLevel.WARNING -> AmberSecondary
        LogLevel.ERROR -> RoseError
        LogLevel.PROGRESS -> CyanPrimary
        LogLevel.JS -> TextPrimary
    }

    val tag = when (entry.level) {
        LogLevel.INFO -> "[INFO]"
        LogLevel.SUCCESS -> "[SUCCESS]"
        LogLevel.WARNING -> "[WARN]"
        LogLevel.ERROR -> "[ERROR]"
        LogLevel.PROGRESS -> "[PROG]"
        LogLevel.JS -> "[JS]"
    }

    val timeStr = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(entry.timestampMs))

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = timeStr,
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
            color = TextMuted,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = tag,
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = entry.message,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = TextPrimary,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
