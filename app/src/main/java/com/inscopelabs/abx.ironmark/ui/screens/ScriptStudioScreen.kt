package com.inscopelabs.abx.ironmark.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.ironmark.model.ExecutionState
import com.inscopelabs.abx.ironmark.ui.theme.CyanPrimary
import com.inscopelabs.abx.ironmark.ui.theme.DarkBorder
import com.inscopelabs.abx.ironmark.ui.theme.DarkSurface
import com.inscopelabs.abx.ironmark.ui.theme.DarkSurfaceVariant
import com.inscopelabs.abx.ironmark.ui.theme.TextMuted
import com.inscopelabs.abx.ironmark.ui.theme.TextPrimary
import com.inscopelabs.abx.ironmark.ui.theme.TextSecondary
import com.inscopelabs.abx.ironmark.viewmodel.IronMarkViewModel

@Composable
fun ScriptStudioScreen(
    viewModel: IronMarkViewModel,
    modifier: Modifier = Modifier
) {
    val selectedScript by viewModel.selectedScript.collectAsState()
    val customJsCode by viewModel.customJsCode.collectAsState()
    val customJsonParams by viewModel.customJsonParams.collectAsState()
    val executionState by viewModel.executionState.collectAsState()

    val templates = listOf(
        IronMarkViewModel.DEFAULT_SPLITTER_SCRIPT,
        IronMarkViewModel.SHA_HASH_SCRIPT,
        IronMarkViewModel.CHAT_RESPONSE_SPLITTER_SCRIPT
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Column {
                Text(
                    text = "Script Studio",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = "Inspect and execute custom JavaScript routines via Android bridge",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        item {
            Text(
                text = "SCRIPT TEMPLATES",
                style = MaterialTheme.typography.labelMedium,
                color = CyanPrimary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(templates) { template ->
                    val isSelected = selectedScript.id == template.id
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { viewModel.selectScriptTemplate(template) }
                            .testTag("template_${template.id}"),
                        color = if (isSelected) CyanPrimary.copy(alpha = 0.2f) else DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) CyanPrimary else DarkBorder
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = template.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isSelected) CyanPrimary else TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = template.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                maxLines = 1
                            )
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "JAVASCRIPT SOURCE CODE",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyanPrimary,
                            letterSpacing = 1.sp
                        )
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Code",
                            tint = CyanPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = customJsCode,
                        onValueChange = { viewModel.setCustomJsCode(it) },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant
                        ),
                        minLines = 8,
                        maxLines = 16,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("js_code_input")
                    )
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "JSON PARAMS PAYLOAD",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyanPrimary,
                            letterSpacing = 1.sp
                        )
                        Icon(
                            imageVector = Icons.Default.DataObject,
                            contentDescription = "Params",
                            tint = CyanPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = customJsonParams,
                        onValueChange = { viewModel.setCustomJsonParams(it) },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant
                        ),
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("js_params_input")
                    )
                }
            }
        }

        item {
            Button(
                onClick = { viewModel.executeCustomScript() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("run_script_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanPrimary,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Run",
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Execute JS Script via Bridge",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (executionState is ExecutionState.Success) {
            val resultState = executionState as ExecutionState.Success
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyanPrimary, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    color = DarkSurface
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "EXECUTION RESULT",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyanPrimary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = resultState.resultJson,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
