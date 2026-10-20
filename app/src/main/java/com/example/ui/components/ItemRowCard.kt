package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ListItemEntity
import com.example.data.model.SubTask
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ItemViewMode {
    MINIMUM, CONDENSED, MAXIMUM
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemRowCard(
    item: ListItemEntity,
    listName: String? = null,
    viewMode: ItemViewMode = ItemViewMode.CONDENSED,
    onToggleCheck: (Boolean) -> Unit,
    onClickItem: () -> Unit,
    onListBadgeClick: (() -> Unit)? = null,
    onToggleSubtask: ((Int, Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val category = item.effectiveCategory
    val isChecked = item.isChecked
    val subtasks = remember(item.subtasksJson) { SubTask.fromJson(item.subtasksJson) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClickItem() }
            .testTag("item_row_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isChecked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            1.5.dp,
            if (isChecked) MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp)) {
            when (viewMode) {
                ItemViewMode.MINIMUM -> {
                    // MINIMUM MODE: Category icon, item title (no truncation, wordwrap), list badge directly beneath title, drag handle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        CategoryIconBox(category = category, isChecked = isChecked, itemId = item.id, onToggleCheck = onToggleCheck)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    lineHeight = 16.sp,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    else MaterialTheme.colorScheme.onSurface
                                ),
                                softWrap = true
                            )
                            if (listName != null) {
                                ListBadge(listName = listName, onClick = onListBadgeClick)
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Drag to reorder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                ItemViewMode.CONDENSED -> {
                    // CONDENSED MODE: Category icon, item title (wordwrap), list badge, due date on its own line below list badge, tags on own line, drag handle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        CategoryIconBox(category = category, isChecked = isChecked, itemId = item.id, onToggleCheck = onToggleCheck)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    lineHeight = 16.sp,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    else MaterialTheme.colorScheme.onSurface
                                ),
                                softWrap = true
                            )

                            // List badge
                            if (listName != null) {
                                ListBadge(listName = listName, onClick = onListBadgeClick)
                            }

                            // Due date on its own line (below list badge and above tags)
                            if (item.dueDate != null && item.dueDate > 0) {
                                DueDateBadge(dueDate = item.dueDate)
                            }

                            // Tags on their own line
                            if (item.tags.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                TagsFlowRow(tagsStr = item.tags)
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Drag to reorder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                ItemViewMode.MAXIMUM -> {
                    // MAXIMUM MODE: Category icon top left, title (wordwrap), list badge, full description,
                    // creation date (text only) on its own line, due date on its own line below creation date, tags on own line, image thumbnail top right, drag handle.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        CategoryIconBox(category = category, isChecked = isChecked, itemId = item.id, onToggleCheck = onToggleCheck)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    lineHeight = 16.sp,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    else MaterialTheme.colorScheme.onSurface
                                ),
                                softWrap = true
                            )
                            if (listName != null) {
                                ListBadge(listName = listName, onClick = onListBadgeClick)
                            }
                        }

                        // Thumbnail at top right of item if image present
                        if (!item.imageUri.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            ) {
                                RenderItemImage(
                                    imageUri = item.imageUri,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Drag to reorder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Full description (no truncation, wordwrap)
                    if (item.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                lineHeight = 15.sp,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            ),
                            softWrap = true
                        )
                    }

                    // Creation date (text only, no calendar icon) on its own line
                    Spacer(modifier = Modifier.height(3.dp))
                    val creationDateStr = "Created " + SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(item.createdAt))
                    Text(
                        text = creationDateStr,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            lineHeight = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )

                    // Due date on its own line below created date
                    if (item.dueDate != null && item.dueDate > 0) {
                        Spacer(modifier = Modifier.height(2.dp))
                        val dueColor = getDueDateColor(item.dueDate)
                        val dueFormattedStr = "Due " + SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(item.dueDate))
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = dueColor.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, dueColor.copy(alpha = 0.4f)),
                            modifier = Modifier.padding(top = 1.dp)
                        ) {
                            Text(
                                text = dueFormattedStr,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.5.sp,
                                    lineHeight = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = dueColor,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.dp)
                            )
                        }
                    }

                    // Tags on their own line
                    if (item.tags.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        TagsFlowRow(tagsStr = item.tags)
                    }
                }
            }

            // Visible Subtasks List inside card (if present and not minimum mode)
            if (subtasks.isNotEmpty() && viewMode != ItemViewMode.MINIMUM) {
                Spacer(modifier = Modifier.height(3.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    subtasks.forEachIndexed { index, st ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.dp)
                        ) {
                            Icon(
                                imageVector = if (st.isCompleted) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = null,
                                tint = if (st.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable {
                                        onToggleSubtask?.invoke(index, !st.isCompleted)
                                    }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = st.title,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    lineHeight = 12.sp,
                                    textDecoration = if (st.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (st.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                ),
                                softWrap = true
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryIconBox(category: String, isChecked: Boolean, itemId: Long, onToggleCheck: (Boolean) -> Unit) {
    when (category) {
        "Task" -> {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isChecked) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    )
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onToggleCheck(!isChecked) }
                    .testTag("checkbox_$itemId"),
                contentAlignment = Alignment.Center
            ) {
                if (isChecked) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Checked",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        "Idea" -> {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF59E0B).copy(alpha = 0.18f))
                    .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "Idea",
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        "Note" -> {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFEC4899).copy(alpha = 0.18f))
                    .border(1.dp, Color(0xFFEC4899).copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = "Note",
                    tint = Color(0xFFDB2777),
                    modifier = Modifier.size(13.dp)
                )
            }
        }
        else -> {
            val iconVector = getVectorForCategory(category)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0284C7).copy(alpha = 0.18f))
                    .border(1.dp, Color(0xFF0284C7).copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = category,
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
private fun ListBadge(listName: String, onClick: (() -> Unit)?) {
    Surface(
        onClick = { onClick?.invoke() },
        shape = RoundedCornerShape(3.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.dp)
        ) {
            Text(
                text = listName,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    lineHeight = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(9.dp)
            )
        }
    }
}

@Composable
private fun DueDateBadge(dueDate: Long) {
    val dueLabel = formatDueDateLabel(dueDate)
    Surface(
        shape = RoundedCornerShape(3.dp),
        color = Color(0xFFF472B6).copy(alpha = 0.2f),
        border = BorderStroke(1.dp, Color(0xFFF472B6).copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                tint = Color(0xFFDB2777),
                modifier = Modifier.size(8.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = dueLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    lineHeight = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDB2777),
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsFlowRow(tagsStr: String) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        tagsStr.split(",", ";").map { it.trim().removePrefix("#") }.filter { it.isNotBlank() }.forEach { tag ->
            Surface(
                shape = RoundedCornerShape(3.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Text(
                    text = "#$tag",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        lineHeight = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.dp)
                )
            }
        }
    }
}

private fun formatDueDateLabel(dueMillis: Long): String {
    val now = Calendar.getInstance()
    val due = Calendar.getInstance().apply { timeInMillis = dueMillis }

    return when {
        now.get(Calendar.YEAR) == due.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == due.get(Calendar.DAY_OF_YEAR) -> "DUE: TODAY"
        now.get(Calendar.YEAR) == due.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) + 1 == due.get(Calendar.DAY_OF_YEAR) -> "DUE: TOMORROW"
        else -> "DUE: " + SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(dueMillis)).uppercase()
    }
}

private fun getDueDateColor(dueMillis: Long): Color {
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
