package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CategoryOption
import com.example.data.model.ListEntity
import com.example.data.model.ListItemEntity
import com.example.data.model.PriorityOption
import com.example.data.model.SubTask
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemPreviewDialog(
    item: ListItemEntity,
    lists: List<ListEntity>,
    categories: List<CategoryOption>,
    priorities: List<PriorityOption>,
    onDismiss: () -> Unit,
    onEdit: (ListItemEntity) -> Unit,
    onDelete: (ListItemEntity) -> Unit,
    onToggleCheck: (Long, Boolean) -> Unit = { _, _ -> },
    onToggleSubtask: (Int, Boolean) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val parentList = remember(lists, item.listId) { lists.find { it.id == item.listId } }
    val category = item.effectiveCategory
    val subtasks = remember(item.subtasksJson) { SubTask.fromJson(item.subtasksJson) }

    // State for Delete Confirmation Prompt
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var neverAskAgainChecked by remember { mutableStateOf(false) }

    val prefs = remember { context.getSharedPreferences("list_widget_prefs", Context.MODE_PRIVATE) }
    val shouldPromptDelete = remember { prefs.getBoolean("confirm_item_delete", true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .testTag("item_preview_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Fixed Header Bar with Close (X)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 12.dp, top = 14.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ITEM PREVIEW",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Preview",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Scrollable Content Pane
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Category, List, and Priority Badges
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Category Badge
                        val catObj = remember(categories, category) { categories.find { it.name.equals(category, ignoreCase = true) } }
                        val catColor = remember(catObj) {
                            try {
                                if (catObj != null) Color(android.graphics.Color.parseColor(catObj.colorHex))
                                else Color(0xFF6366F1)
                            } catch (_: Exception) { Color(0xFF6366F1) }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = catColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, catColor.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val catIcon = getVectorForCategory(category, catObj?.iconKey ?: "tag")
                                Icon(
                                    imageVector = catIcon,
                                    contentDescription = null,
                                    tint = catColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = catColor
                                    )
                                )
                            }
                        }

                        // List Badge (if list exists)
                        if (parentList != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = parentList.name,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        // Priority Tag (if set and not NONE)
                        if (item.priority.isNotBlank() && !item.priority.equals("NONE", ignoreCase = true)) {
                            val priObj = remember(priorities, item.priority) {
                                priorities.find { it.id.equals(item.priority, ignoreCase = true) || it.label.equals(item.priority, ignoreCase = true) }
                            }
                            val priColor = remember(priObj, item.priority) {
                                try {
                                    if (priObj != null) Color(android.graphics.Color.parseColor(priObj.colorHex))
                                    else Color(0xFFEF4444)
                                } catch (_: Exception) { Color(0xFFEF4444) }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = priColor.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, priColor.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = "${priObj?.label ?: item.priority} Priority",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = priColor
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Item Title
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    // Hero Image (ONLY if image present!)
                    if (!item.imageUri.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        ) {
                            RenderItemImage(
                                imageUri = item.imageUri,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Description Section with Rich Text (ONLY shown if description is set!)
                    if (item.description.isNotBlank()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "DESCRIPTION",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.padding(12.dp)) {
                                    FormattedRichText(text = item.description)
                                }
                            }
                        }
                    }

                    // Dates Section (Created Date & Due Date if set)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val createdStr = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(item.createdAt))
                        Text(
                            text = "Created: $createdStr",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        if (item.dueDate != null && item.dueDate > 0) {
                            val dueColor = getDueDateColorForPreview(item.dueDate)
                            val dueStr = "Due " + SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(item.dueDate))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = dueColor.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, dueColor.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = dueColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = dueStr,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = dueColor
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Tags Section (ONLY shown if tags are set!)
                    if (item.tags.isNotBlank()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "TAGS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                item.tags.split(",", ";").map { it.trim().removePrefix("#") }.filter { it.isNotBlank() }.forEach { tag ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                    ) {
                                        Text(
                                            text = "#$tag",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Subtasks Section (ONLY shown if subtasks are set!)
                    if (subtasks.isNotEmpty()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "SUBTASKS (${subtasks.count { it.isCompleted }}/${subtasks.size})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                subtasks.forEachIndexed { idx, st ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = if (st.isCompleted) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                            contentDescription = null,
                                            tint = if (st.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clickable { onToggleSubtask(idx, !st.isCompleted) }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = st.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                textDecoration = if (st.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                                color = if (st.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // FIXED ACTION BUTTONS ALONG BOTTOM (Delete & Edit - User NEVER has to scroll!)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Delete Button
                    OutlinedButton(
                        onClick = {
                            if (shouldPromptDelete) {
                                showDeleteConfirmDialog = true
                            } else {
                                onDelete(item)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("preview_delete_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }

                    // Edit Button
                    Button(
                        onClick = {
                            onEdit(item)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("preview_edit_btn")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // DELETION CONFIRMATION DIALOG WITH "NEVER ASK AGAIN" CHECKBOX
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Item?", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Are you sure you want to delete this item?", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { neverAskAgainChecked = !neverAskAgainChecked }
                    ) {
                        Checkbox(
                            checked = neverAskAgainChecked,
                            onCheckedChange = { neverAskAgainChecked = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Never ask again", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (neverAskAgainChecked) {
                            prefs.edit().putBoolean("confirm_item_delete", false).apply()
                        }
                        showDeleteConfirmDialog = false
                        onDelete(item)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FormattedRichText(
    text: String,
    modifier: Modifier = Modifier
) {
    val lines = remember(text) { text.split("\n") }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        lines.forEach { rawLine ->
            val trimmed = rawLine.trim()
            when {
                trimmed.startsWith("# ") -> {
                    Text(
                        text = buildAnnotatedStringWithFormatting(trimmed.removePrefix("# ")),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                trimmed.startsWith("## ") -> {
                    Text(
                        text = buildAnnotatedStringWithFormatting(trimmed.removePrefix("## ")),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                trimmed.startsWith("### ") -> {
                    Text(
                        text = buildAnnotatedStringWithFormatting(trimmed.removePrefix("### ")),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                trimmed.startsWith("> ") -> {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = buildAnnotatedStringWithFormatting(trimmed.removePrefix("> ")),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                trimmed.startsWith("• ") || trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    val bulletContent = trimmed.substring(2).trim()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = buildAnnotatedStringWithFormatting(bulletContent),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 18.sp
                            )
                        )
                    }
                }
                trimmed.startsWith("  • ") || trimmed.startsWith("  - ") || trimmed.startsWith("  * ") -> {
                    val bulletContent = trimmed.substring(4).trim()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 18.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "◦",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = buildAnnotatedStringWithFormatting(bulletContent),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 18.sp
                            )
                        )
                    }
                }
                trimmed.isBlank() -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                else -> {
                    Text(
                        text = buildAnnotatedStringWithFormatting(rawLine),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }
    }
}

private fun buildAnnotatedStringWithFormatting(text: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var i = 0
    while (i < text.length) {
        if (i + 1 < text.length && text[i] == '*' && text[i + 1] == '*') {
            val end = text.indexOf("**", i + 2)
            if (end != -1) {
                val boldContent = text.substring(i + 2, end)
                builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(boldContent)
                }
                i = end + 2
                continue
            }
        }
        if (text[i] == '*') {
            val end = text.indexOf('*', i + 1)
            if (end != -1) {
                val italicContent = text.substring(i + 1, end)
                builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(italicContent)
                }
                i = end + 1
                continue
            }
        }
        if (text[i] == '`') {
            val end = text.indexOf('`', i + 1)
            if (end != -1) {
                val codeContent = text.substring(i + 1, end)
                builder.withStyle(SpanStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)) {
                    append(codeContent)
                }
                i = end + 1
                continue
            }
        }
        builder.append(text[i])
        i++
    }
    return builder.toAnnotatedString()
}

private fun getDueDateColorForPreview(dueMillis: Long): Color {
    val calendar = Calendar.getInstance()

    // Start of Today
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val startOfToday = calendar.timeInMillis

    // End of Today
    calendar.set(Calendar.HOUR_OF_DAY, 23)
    calendar.set(Calendar.MINUTE, 59)
    calendar.set(Calendar.SECOND, 59)
    calendar.set(Calendar.MILLISECOND, 999)
    val endOfToday = calendar.timeInMillis

    // End of Next Two Days (Tomorrow and Day After Tomorrow)
    calendar.add(Calendar.DAY_OF_MONTH, 2)
    val endOfTwoDays = calendar.timeInMillis

    return when {
        dueMillis < startOfToday -> Color(0xFFEF4444) // Red: past due
        dueMillis <= endOfToday -> Color(0xFF10B981)  // Green: due today
        dueMillis <= endOfTwoDays -> Color(0xFFF59E0B) // Yellow: due in next 2 days
        else -> Color(0xFF3B82F6)                      // Blue: due > 2 days in future
    }
}
