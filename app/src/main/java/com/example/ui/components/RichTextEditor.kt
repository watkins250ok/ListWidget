package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatClear
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RichTextEditor(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Write details, notes, or indented bullet lists..."
) {
    var textFieldValue by remember(value) {
        mutableStateOf(
            TextFieldValue(
                text = value,
                selection = TextRange(value.length)
            )
        )
    }

    var showLinkDialog by remember { mutableStateOf(false) }
    var linkText by remember { mutableStateOf("") }
    var linkUrl by remember { mutableStateOf("https://") }

    fun updateValueAndCursor(newText: String, cursorIndex: Int) {
        val newCursor = cursorIndex.coerceIn(0, newText.length)
        val tfv = TextFieldValue(text = newText, selection = TextRange(newCursor))
        textFieldValue = tfv
        onValueChange(newText)
    }

    fun insertPrefixAndPositionCursor(prefix: String) {
        val currentText = textFieldValue.text
        val selStart = textFieldValue.selection.start

        val newText: String
        val newCursorPos: Int

        if (currentText.isBlank()) {
            newText = prefix
            newCursorPos = prefix.length
        } else {
            val lineStart = currentText.lastIndexOf('\n', (selStart - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
            val beforeLine = currentText.substring(0, lineStart)
            val afterLine = currentText.substring(lineStart)
            newText = "$beforeLine$prefix$afterLine"
            newCursorPos = lineStart + prefix.length
        }

        updateValueAndCursor(newText, newCursorPos)
    }

    fun wrapOrInsert(prefix: String, suffix: String, defaultPlaceholder: String) {
        val text = textFieldValue.text
        val selStart = textFieldValue.selection.start
        val selEnd = textFieldValue.selection.end

        val selectedText = if (selStart != selEnd) text.substring(selStart, selEnd) else defaultPlaceholder
        val newText = if (selStart != selEnd) {
            text.substring(0, selStart) + prefix + selectedText + suffix + text.substring(selEnd)
        } else {
            if (text.isBlank()) "$prefix$selectedText$suffix" else "$text\n$prefix$selectedText$suffix"
        }

        val newCursor = newText.length
        updateValueAndCursor(newText, newCursor)
    }

    fun handleTextChange(newValue: TextFieldValue) {
        val oldText = textFieldValue.text
        val newText = newValue.text

        // 1. Check if Enter key was pressed (\n inserted)
        if (newText.length == oldText.length + 1 && newValue.selection.start == newValue.selection.end) {
            val cursorIndex = newValue.selection.start
            if (cursorIndex > 0 && newText[cursorIndex - 1] == '\n') {
                val textBeforeNewline = newText.substring(0, cursorIndex - 1)
                val lastLineStart = textBeforeNewline.lastIndexOf('\n').let { if (it == -1) 0 else it + 1 }
                val lastLine = textBeforeNewline.substring(lastLineStart)

                val bulletRegex = Regex("""^(\s*[•\*\-]\s+)""")
                val numberRegex = Regex("""^(\s*)(\d+)(\.\s+)""")

                val bulletMatch = bulletRegex.find(lastLine)
                val numberMatch = numberRegex.find(lastLine)

                if (bulletMatch != null) {
                    val bulletPrefix = bulletMatch.groupValues[1]
                    val lineContent = lastLine.substring(bulletPrefix.length).trim()

                    if (lineContent.isEmpty()) {
                        // Pressing Enter on empty bullet line exits bullet list mode!
                        val updatedText = newText.substring(0, lastLineStart) + newText.substring(cursorIndex)
                        updateValueAndCursor(updatedText, lastLineStart)
                        return
                    } else {
                        // Auto-continue bullet list on next line
                        val updatedText = newText.substring(0, cursorIndex) + bulletPrefix + newText.substring(cursorIndex)
                        updateValueAndCursor(updatedText, cursorIndex + bulletPrefix.length)
                        return
                    }
                } else if (numberMatch != null) {
                    val indent = numberMatch.groupValues[1]
                    val currentNum = numberMatch.groupValues[2].toIntOrNull() ?: 1
                    val dotSpace = numberMatch.groupValues[3]
                    val fullPrefix = numberMatch.groupValues[0]
                    val lineContent = lastLine.substring(fullPrefix.length).trim()

                    if (lineContent.isEmpty()) {
                        // Pressing Enter on empty number line exits numbered list mode!
                        val updatedText = newText.substring(0, lastLineStart) + newText.substring(cursorIndex)
                        updateValueAndCursor(updatedText, lastLineStart)
                        return
                    } else {
                        // Auto-continue numbered list on next line with incremented N+1!
                        val nextNum = currentNum + 1
                        val insertion = "$indent$nextNum$dotSpace"
                        val updatedText = newText.substring(0, cursorIndex) + insertion + newText.substring(cursorIndex)
                        updateValueAndCursor(updatedText, cursorIndex + insertion.length)
                        return
                    }
                }
            }
        }

        // 2. Check if Backspace was pressed on an empty bullet / number line
        if (newText.length < oldText.length) {
            val cursorIndex = newValue.selection.start
            val lineStart = oldText.lastIndexOf('\n', cursorIndex.coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
            val oldLineEnd = oldText.indexOf('\n', lineStart).let { if (it == -1) oldText.length else it }
            val oldLine = oldText.substring(lineStart, oldLineEnd)

            val bulletRegex = Regex("""^(\s*[•\*\-]\s+)""")
            val numberRegex = Regex("""^(\s*\d+\.\s+)""")

            val bulletMatch = bulletRegex.find(oldLine)
            val numberMatch = numberRegex.find(oldLine)

            if (bulletMatch != null || numberMatch != null) {
                val prefixLen = (bulletMatch ?: numberMatch)?.value?.length ?: 0
                if (prefixLen > 0 && cursorIndex <= lineStart + prefixLen) {
                    val cleanedText = oldText.substring(0, lineStart) + oldText.substring((lineStart + prefixLen).coerceAtMost(oldText.length))
                    updateValueAndCursor(cleanedText, lineStart)
                    return
                }
            }
        }

        textFieldValue = newValue
        onValueChange(newText)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
    ) {
        // Formatting Toolbar (FlowRow wraps overflow buttons onto line 2 without scrolling)
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            ToolbarButton(icon = Icons.Default.FormatBold, contentDesc = "Bold") {
                wrapOrInsert("**", "**", "Bold Text")
            }
            ToolbarButton(icon = Icons.Default.FormatItalic, contentDesc = "Italic") {
                wrapOrInsert("*", "*", "Italic Text")
            }
            ToolbarButton(icon = Icons.Default.FormatUnderlined, contentDesc = "Underline") {
                wrapOrInsert("<u>", "</u>", "Underlined")
            }
            ToolbarButton(icon = Icons.Default.FormatStrikethrough, contentDesc = "Strike") {
                wrapOrInsert("~~", "~~", "Strikethrough")
            }
            ToolbarTextButton(label = "H1", contentDesc = "Heading 1") {
                insertPrefixAndPositionCursor("# ")
            }
            ToolbarTextButton(label = "H2", contentDesc = "Heading 2") {
                insertPrefixAndPositionCursor("## ")
            }
            ToolbarButton(icon = Icons.AutoMirrored.Filled.FormatListBulleted, contentDesc = "Bullet List") {
                insertPrefixAndPositionCursor("• ")
            }
            ToolbarButton(icon = Icons.Default.FormatListNumbered, contentDesc = "Numbered List") {
                insertPrefixAndPositionCursor("1. ")
            }
            ToolbarButton(icon = Icons.Default.Link, contentDesc = "Hyperlink") {
                val selStart = textFieldValue.selection.start
                val selEnd = textFieldValue.selection.end
                linkText = if (selStart != selEnd) textFieldValue.text.substring(selStart, selEnd) else ""
                linkUrl = "https://"
                showLinkDialog = true
            }
            ToolbarButton(icon = Icons.Default.FormatQuote, contentDesc = "Quote") {
                insertPrefixAndPositionCursor("> ")
            }
            ToolbarButton(icon = Icons.Default.Code, contentDesc = "Code") {
                wrapOrInsert("`", "`", "code")
            }
            ToolbarButton(icon = Icons.Default.FormatClear, contentDesc = "Clear") {
                updateValueAndCursor("", 0)
            }
        }

        // Editor Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .heightIn(min = 120.dp, max = 240.dp)
        ) {
            if (textFieldValue.text.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                )
            }
            BasicTextField(
                value = textFieldValue,
                onValueChange = ::handleTextChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
            )
        }
    }

    // Hyperlink Dialog
    if (showLinkDialog) {
        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            title = { Text("Insert Hyperlink", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = linkText,
                        onValueChange = { linkText = it },
                        label = { Text("Link Text") },
                        placeholder = { Text("e.g. Visit Website") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = linkUrl,
                        onValueChange = { linkUrl = it },
                        label = { Text("URL Link") },
                        placeholder = { Text("https://example.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val textToInsert = linkText.ifBlank { "Link" }
                        val urlToInsert = if (linkUrl.isBlank()) "https://" else linkUrl.trim()
                        val markdownLink = "[$textToInsert]($urlToInsert)"

                        val currentText = textFieldValue.text
                        val selStart = textFieldValue.selection.start
                        val selEnd = textFieldValue.selection.end

                        val newText = if (selStart != selEnd) {
                            currentText.substring(0, selStart) + markdownLink + currentText.substring(selEnd)
                        } else {
                            if (currentText.isBlank()) markdownLink else "$currentText $markdownLink"
                        }

                        val newCursor = newText.length
                        updateValueAndCursor(newText, newCursor)
                        showLinkDialog = false
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Insert Link")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLinkDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    contentDesc: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(34.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ToolbarTextButton(
    label: String,
    contentDesc: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = Color.Transparent,
        modifier = Modifier.size(34.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
