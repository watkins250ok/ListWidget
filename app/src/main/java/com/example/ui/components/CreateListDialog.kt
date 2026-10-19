package com.example.ui.components

import com.example.ui.theme.PrimaryPurple
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ListEntity

val LIST_COLOR_PRESETS = listOf(
    "#7C3AED", "#EC4899", "#0284C7", "#059669", "#D97706", "#DB2777", "#6366F1", "#14B8A6"
)

@Composable
fun CreateListDialog(
    listToEdit: ListEntity? = null,
    onDismiss: () -> Unit,
    onSave: (ListEntity) -> Unit,
    onDelete: ((ListEntity) -> Unit)? = null
) {
    val isEditMode = listToEdit != null

    var name by remember { mutableStateOf(listToEdit?.name ?: "") }
    var description by remember { mutableStateOf(listToEdit?.description ?: "") }
    var iconName by remember { mutableStateOf(listToEdit?.iconName ?: "checklist") }
    var colorHex by remember { mutableStateOf(listToEdit?.colorHex ?: "#7C3AED") }
    var autoDeleteMode by remember { mutableStateOf(listToEdit?.autoDeleteMode ?: "NEVER") }
    var hideCheckedItems by remember { mutableStateOf(listToEdit?.hideCheckedItems ?: false) }

    // Section Expand/Collapse State
    var iconPickerExpanded by remember { mutableStateOf(false) }
    var autoDeleteMenuExpanded by remember { mutableStateOf(false) }

    val selectedColor = remember(colorHex) {
        try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (_: Exception) {
            PrimaryPurple
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 640.dp)
                .clip(RoundedCornerShape(24.dp))
                .testTag("create_list_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Fixed Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isEditMode) "Edit List" else "Create New List",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Scrollable Middle Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Section 1: List Name
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "LIST NAME *",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                placeholder = { Text("e.g. Work Projects, Groceries...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("list_name_input"),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }
                    }

                    // Section 2: List Icon (Expandable, Collapsed by Default)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { iconPickerExpanded = !iconPickerExpanded },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = getVectorForIconName(iconName),
                                        contentDescription = null,
                                        tint = selectedColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "LIST ICON",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                letterSpacing = 1.sp
                                            )
                                        )
                                        Text(
                                            text = "Selected: ${iconName.replaceFirstChar { it.uppercase() }}",
                                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (iconPickerExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Expand/Collapse Icon Picker",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (iconPickerExpanded) {
                                Spacer(modifier = Modifier.height(12.dp))
                                IconPickerGrid(
                                    selectedIconName = iconName,
                                    onSelectIcon = {
                                        iconName = it
                                    },
                                    accentColor = selectedColor
                                )
                            }
                        }
                    }

                    // Section 3: Accent Color
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "ACCENT COLOR",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LIST_COLOR_PRESETS.forEach { hex ->
                                    val color = Color(android.graphics.Color.parseColor(hex))
                                    val isSelected = colorHex.equals(hex, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { colorHex = hex }
                                    )
                                }
                            }
                        }
                    }

                    // Section 4: Description (Text: "DESCRIPTION")
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "DESCRIPTION",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                placeholder = { Text("Add notes or purpose for this list...") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                minLines = 2,
                                maxLines = 4
                            )
                        }
                    }

                    // Section 5: Auto-Delete Checked Items (Full width dropdown + arrow)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "AUTO-DELETE CHECKED ITEMS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Box {
                                Surface(
                                    onClick = { autoDeleteMenuExpanded = true },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = when (autoDeleteMode) {
                                                "IMMEDIATELY" -> "Delete Immediately"
                                                "AFTER_24H" -> "Delete after 24 hours"
                                                "AFTER_1W" -> "Delete after 1 week"
                                                else -> "Never (Keep Checked Items)"
                                            },
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Dropdown Menu",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = autoDeleteMenuExpanded,
                                    onDismissRequest = { autoDeleteMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Never (Keep Checked Items)") },
                                        onClick = {
                                            autoDeleteMode = "NEVER"
                                            autoDeleteMenuExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete Immediately") },
                                        onClick = {
                                            autoDeleteMode = "IMMEDIATELY"
                                            autoDeleteMenuExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete after 24 hours") },
                                        onClick = {
                                            autoDeleteMode = "AFTER_24H"
                                            autoDeleteMenuExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete after 1 week") },
                                        onClick = {
                                            autoDeleteMode = "AFTER_1W"
                                            autoDeleteMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Section 6: Hide Checked Items (Toggle on same line as header, clear active/inactive visual mode)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (hideCheckedItems) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.5.dp,
                            if (hideCheckedItems) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "HIDE CHECKED ITEMS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                            )
                            Switch(
                                checked = hideCheckedItems,
                                onCheckedChange = { hideCheckedItems = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }

                // FIXED ACTION BUTTONS ALONG BOTTOM (Delete, Cancel, Save / Create - ALWAYS visible!)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (isEditMode && onDelete != null && listToEdit != null) {
                        OutlinedButton(
                            onClick = {
                                onDelete(listToEdit)
                                onDismiss()
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", color = MaterialTheme.colorScheme.error, maxLines = 1)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    val now = System.currentTimeMillis()
                                    val newList = ListEntity(
                                        id = listToEdit?.id ?: 0L,
                                        name = name.trim(),
                                        iconName = iconName,
                                        customIconUri = null,
                                        description = description.ifBlank { null },
                                        createdAt = listToEdit?.createdAt ?: now,
                                        updatedAt = now,
                                        colorHex = colorHex,
                                        autoDeleteMode = autoDeleteMode,
                                        hideCheckedItems = hideCheckedItems
                                    )
                                    onSave(newList)
                                    onDismiss()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("save_list_button")
                        ) {
                            Text(
                                text = if (isEditMode) "Save Changes" else "Create List",
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}
