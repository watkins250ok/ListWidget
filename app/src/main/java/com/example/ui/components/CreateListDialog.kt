package com.example.ui.components

import android.net.Uri
import com.example.ui.theme.PrimaryPurple
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
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
                .heightIn(max = 620.dp)
                .clip(RoundedCornerShape(24.dp))
                .testTag("create_list_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
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

                Spacer(modifier = Modifier.height(16.dp))

                // List Name
                Text(
                    text = "LIST NAME *",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

                Spacer(modifier = Modifier.height(14.dp))

                // List Icon Options
                Text(
                    text = "LIST ICON",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))

                IconPickerGrid(
                    selectedIconName = iconName,
                    onSelectIcon = { iconName = it },
                    accentColor = selectedColor
                )

                Spacer(modifier = Modifier.height(14.dp))

                // List Accent Color
                Text(
                    text = "ACCENT COLOR",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
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

                Spacer(modifier = Modifier.height(14.dp))

                // Optional Description Box
                Text(
                    text = "OPTIONAL DESCRIPTION",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

                Spacer(modifier = Modifier.height(14.dp))

                // Auto-delete checked items setting
                Text(
                    text = "AUTO-DELETE CHECKED ITEMS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box {
                    Surface(
                        onClick = { autoDeleteMenuExpanded = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
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
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text("Change ⌄", style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.primary))
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

                Spacer(modifier = Modifier.height(14.dp))

                // Hide Checked Items Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Hide checked items", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        Text("Collapse completed tasks from view", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                    }
                    Switch(
                        checked = hideCheckedItems,
                        onCheckedChange = { hideCheckedItems = it }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (isEditMode && onDelete != null && listToEdit != null) {
                        OutlinedButton(
                            onClick = {
                                onDelete(listToEdit)
                                onDismiss()
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
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
