package dev.chandradsl.m3ecanvas.editor.codegen

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chandradsl.m3ecanvas.domain.model.M3EProject

enum class CodeExportTab(val displayName: String) {
    COMPOSE_CODE("Kotlin Compose"),
    FULL_PROJECT("Full Project (Multi-File)"),
    AI_PROMPT("AI Prompt Spec")
}

@Composable
fun CodeExportPanel(
    project: M3EProject,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(CodeExportTab.COMPOSE_CODE) }
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    val fullProjectFiles = remember(project) {
        ProjectCodeExporter.exportProject(project)
    }
    var selectedFileIndex by remember { mutableStateOf(0) }

    val currentFile = fullProjectFiles.getOrElse(selectedFileIndex) { fullProjectFiles.first() }

    val codeText = remember(project, selectedTab, selectedFileIndex) {
        when (selectedTab) {
            CodeExportTab.COMPOSE_CODE -> ComposeCodeGenerator.generateFile(project)
            CodeExportTab.FULL_PROJECT -> currentFile.content
            CodeExportTab.AI_PROMPT -> AIPromptGenerator.generate(project)
        }
    }

    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(2000)
            copied = false
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF14151A),
        contentColor = Color(0xFFE6E6EB)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Tab Switcher
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CodeExportTab.entries.forEach { tab ->
                        FilterChip(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            label = { Text(tab.displayName, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = Color(0xFF22242C),
                                labelColor = Color(0xFFB0B3C0)
                            )
                        )
                    }
                }

                // Action buttons (Copy + Close)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedTab == CodeExportTab.FULL_PROJECT) {
                        TextButton(
                            onClick = {
                                val allFilesCombined = fullProjectFiles.joinToString("\n\n" + "=".repeat(60) + "\n\n") { file ->
                                    "// File: ${file.path}\n\n${file.content}"
                                }
                                clipboardManager.setText(AnnotatedString(allFilesCombined))
                                copied = true
                            }
                        ) {
                            Text("Copy All Files", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(codeText))
                            copied = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (copied) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (copied) Icons.Outlined.Done else Icons.Outlined.ContentCopy,
                            contentDescription = "Copy",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (copied) "Copied!" else if (selectedTab == CodeExportTab.FULL_PROJECT) "Copy ${currentFile.filename}" else "Copy ${selectedTab.displayName}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = Color(0xFFB0B3C0)
                        )
                    }
                }
            }

            // Sub-header for Multi-File Project navigation
            if (selectedTab == CodeExportTab.FULL_PROJECT) {
                Surface(
                    color = Color(0xFF1B1C22),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = null,
                            tint = Color(0xFF8B8E9D),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Project Files:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF8B8E9D)
                        )
                        fullProjectFiles.forEachIndexed { index, file ->
                            InputChip(
                                selected = selectedFileIndex == index,
                                onClick = { selectedFileIndex = index },
                                label = { Text(file.filename, fontSize = 11.sp) },
                                colors = InputChipDefaults.inputChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    containerColor = Color(0xFF262832),
                                    labelColor = Color(0xFFB0B3C0)
                                )
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF262832))

            // Code Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF111216))
                    .padding(16.dp)
            ) {
                val vScrollState = rememberScrollState()
                val hScrollState = rememberScrollState()

                SelectionContainer {
                    Text(
                        text = codeText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = Color(0xFFCE9178),
                        modifier = Modifier
                            .verticalScroll(vScrollState)
                            .horizontalScroll(hScrollState)
                            .fillMaxSize()
                    )
                }
            }
        }
    }
}
