package com.example.ui.components

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
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
fun ItemPopupDialog(
    itemToEdit: ListItemEntity? = null,
    defaultListId: Long = 0L,
    lists: List<ListEntity>,
    categories: List<CategoryOption> = CategoryOption.DEFAULT_CATEGORIES,
    priorities: List<PriorityOption> = PriorityOption.DEFAULT_PRIORITIES,
    onDismiss: () -> Unit,
    onSave: (ListItemEntity) -> Unit,
    onDelete: ((ListItemEntity) -> Unit)? = null,
    onAddCustomCategory: ((name: String, iconKey: String, colorHex: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val isEditMode = itemToEdit != null

    var selectedListId by remember {
        mutableStateOf(itemToEdit?.listId ?: if (defaultListId > 0) defaultListId else lists.firstOrNull()?.id ?: 0L)
    }
    var selectedCategory by remember {
        mutableStateOf(
            itemToEdit?.category?.takeIf { it.isNotBlank() }
                ?: if (itemToEdit?.type == "NOTE") "Note" else "Task"
        )
    }
    var itemType by remember {
        mutableStateOf(
            if (selectedCategory == "Task") "TASK" else "NOTE"
        )
    }
    var title by remember { mutableStateOf(itemToEdit?.title ?: "") }
    var section by remember { mutableStateOf(itemToEdit?.section ?: "") }
    var description by remember { mutableStateOf(itemToEdit?.description ?: "") }
    var attachedImageUri by remember { mutableStateOf(itemToEdit?.imageUri) }
    var isChecked by remember { mutableStateOf(itemToEdit?.isChecked ?: false) }
    var dueDate by remember { mutableStateOf(itemToEdit?.dueDate) }
    var selectedPriority by remember { mutableStateOf(itemToEdit?.priority ?: "NONE") }
    var tagInput by remember { mutableStateOf("") }
    var tagsList by remember {
        mutableStateOf(
            itemToEdit?.tags?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
        )
    }
    var subtasks by remember {
        mutableStateOf(SubTask.fromJson(itemToEdit?.subtasksJson ?: "[]"))
    }
    var newSubtaskTitle by remember { mutableStateOf("") }

    var listDropdownExpanded by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var showNewCategoryInput by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryIcon by remember { mutableStateOf("tag") }
    var newCategoryColor by remember { mutableStateOf("#0284C7") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            attachedImageUri = uri.toString()
        }
    }

    val currentList = lists.find { it.id == selectedListId } ?: lists.firstOrNull()
    val creationDateFormatted = remember(itemToEdit) {
        val time = itemToEdit?.createdAt ?: System.currentTimeMillis()
        SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.getDefault()).format(Date(time))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(28.dp))
                .testTag("item_popup_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Header Icon Badge
                    val activeCategoryOption = remember(categories, selectedCategory) {
                        categories.find { it.name.equals(selectedCategory, ignoreCase = true) }
                    }
                    val headerIcon = getVectorForCategory(selectedCategory, activeCategoryOption?.iconKey)
                    val headerColor = activeCategoryOption?.let {
                        try { Color(android.graphics.Color.parseColor(it.colorHex)) } catch (_: Exception) { null }
                    } ?: when (selectedCategory) {
                        "Task" -> MaterialTheme.colorScheme.primary
                        "Idea" -> Color(0xFFF59E0B)
                        "Note" -> Color(0xFFEC4899)
                        else -> Color(0xFF0284C7)
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(headerColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = headerIcon,
                            contentDescription = null,
                            tint = headerColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEditMode) "Edit Item" else "New Item",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // List dropdown
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { listDropdownExpanded = true }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "List: ",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Text(
                                    text = currentList?.name ?: "Select List",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Select List",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = listDropdownExpanded,
                                onDismissRequest = { listDropdownExpanded = false }
                            ) {
                                lists.forEach { listEntity ->
                                    DropdownMenuItem(
                                        text = { Text(listEntity.name) },
                                        onClick = {
                                            selectedListId = listEntity.id
                                            listDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_popup_button")) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // CATEGORY SELECTION (Dropdown & Chips)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ITEM CATEGORY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    // Category Dropdown Anchor
                    Box {
                        Surface(
                            onClick = { categoryDropdownExpanded = true },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.testTag("category_dropdown_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Category: $selectedCategory ▾",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                val catIcon = getVectorForCategory(cat.name, cat.iconKey)
                                val catColor = try {
                                    Color(android.graphics.Color.parseColor(cat.colorHex))
                                } catch (_: Exception) {
                                    MaterialTheme.colorScheme.primary
                                }
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    leadingIcon = {
                                        Icon(catIcon, contentDescription = null, tint = catColor, modifier = Modifier.size(18.dp))
                                    },
                                    onClick = {
                                        selectedCategory = cat.name
                                        itemType = if (cat.name == "Task") "TASK" else "NOTE"
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("+ Create Category") },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    categoryDropdownExpanded = false
                                    showNewCategoryInput = true
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Quick Category Selector Cards/Chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory.equals(cat.name, ignoreCase = true)
                        val catIcon = getVectorForCategory(cat.name, cat.iconKey)
                        val catColor = try {
                            Color(android.graphics.Color.parseColor(cat.colorHex))
                        } catch (_: Exception) {
                            MaterialTheme.colorScheme.primary
                        }

                        Surface(
                            onClick = {
                                selectedCategory = cat.name
                                itemType = if (cat.name == "Task") "TASK" else "NOTE"
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) catColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) catColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = catIcon,
                                    contentDescription = null,
                                    tint = if (isSelected) catColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) catColor else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }

                    // Create Category Quick Button
                    Surface(
                        onClick = { showNewCategoryInput = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary))
                        }
                    }
                }

                // New Category Rich Creation Card
                if (showNewCategoryInput) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Create New Category",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                )
                                IconButton(
                                    onClick = { showNewCategoryInput = false },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = newCategoryName,
                                onValueChange = { newCategoryName = it },
                                label = { Text("Category Name") },
                                placeholder = { Text("e.g. Work, Fitness, Books...") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Choose Icon",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            IconPickerGrid(
                                selectedIconName = newCategoryIcon,
                                onSelectIcon = { newCategoryIcon = it },
                                accentColor = try { Color(android.graphics.Color.parseColor(newCategoryColor)) } catch (_: Exception) { MaterialTheme.colorScheme.primary }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Choose Color",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val categoryColorPresets = listOf(
                                "#6366F1", "#F59E0B", "#EC4899", "#10B981",
                                "#3B82F6", "#8B5CF6", "#0284C7", "#EF4444", "#14B8A6", "#F97316"
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                categoryColorPresets.forEach { colorHex ->
                                    val c = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (_: Exception) { Color.Gray }
                                    val isColorSelected = newCategoryColor.equals(colorHex, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(c)
                                            .border(
                                                width = if (isColorSelected) 3.dp else 1.dp,
                                                color = if (isColorSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { newCategoryColor = colorHex },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isColorSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = { showNewCategoryInput = false },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Cancel")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (newCategoryName.isNotBlank()) {
                                            val trimmed = newCategoryName.trim()
                                            onAddCustomCategory?.invoke(trimmed, newCategoryIcon, newCategoryColor)
                                            selectedCategory = trimmed
                                            itemType = if (trimmed.equals("Task", ignoreCase = true)) "TASK" else "NOTE"
                                            newCategoryName = ""
                                            showNewCategoryInput = false
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Save Category")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // TITLE Field
                Text(
                    text = "TITLE *",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("e.g. Minimalist habit tracking widget with heatmaps") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_title_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // SECTION Field
                Text(
                    text = "SECTION (OPTIONAL)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = section,
                    onValueChange = { section = it },
                    placeholder = { Text("e.g. Milestones, To Do, Personal, General...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_section_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // DESCRIPTION & NOTES (RICH TEXT)
                Text(
                    text = "DESCRIPTION & NOTES (RICH TEXT)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                RichTextEditor(
                    value = description,
                    onValueChange = { description = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ATTACHED IMAGE Section
                Text(
                    text = "ATTACHED IMAGE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (attachedImageUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    ) {
                        RenderItemImage(
                            imageUri = attachedImageUri!!,
                            modifier = Modifier.fillMaxSize()
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                modifier = Modifier.size(36.dp),
                                onClick = { photoPickerLauncher.launch("image/*") }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = "Change Image",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                                modifier = Modifier.size(36.dp),
                                onClick = { attachedImageUri = null }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove Image",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload Device Photo")
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // CREATION DATE & DUE DATE ROW
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Creation Date (Auto)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CREATION DATE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = creationDateFormatted,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                maxLines = 1
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "Auto",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Due Date (Optional)
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text(
                            text = "DUE DATE (OPTIONAL)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val dueDateText = if (dueDate != null && dueDate!! > 0) {
                            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(dueDate!!))
                        } else {
                            "dd.mm.yyyy"
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .clickable {
                                    val cal = Calendar.getInstance()
                                    if (dueDate != null) cal.timeInMillis = dueDate!!
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            val c = Calendar.getInstance().apply {
                                                set(y, m, d, 23, 59, 59)
                                            }
                                            dueDate = c.timeInMillis
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = dueDateText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (dueDate != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Pick Due Date",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Quick Chips: [Today] [Tomorrow] [Next Week]
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            QuickDateChip("Today") {
                                val c = Calendar.getInstance()
                                c.set(Calendar.HOUR_OF_DAY, 23)
                                c.set(Calendar.MINUTE, 59)
                                dueDate = c.timeInMillis
                            }
                            QuickDateChip("Tomorrow") {
                                val c = Calendar.getInstance()
                                c.add(Calendar.DAY_OF_MONTH, 1)
                                c.set(Calendar.HOUR_OF_DAY, 23)
                                c.set(Calendar.MINUTE, 59)
                                dueDate = c.timeInMillis
                            }
                            QuickDateChip("Next Week") {
                                val c = Calendar.getInstance()
                                c.add(Calendar.DAY_OF_MONTH, 7)
                                c.set(Calendar.HOUR_OF_DAY, 23)
                                c.set(Calendar.MINUTE, 59)
                                dueDate = c.timeInMillis
                            }
                            if (dueDate != null) {
                                QuickDateChip("✕") { dueDate = null }
                            }
                        }

                        Text(
                            text = "* No due date selected — this field will be hidden in the widget.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            ),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // PRIORITY & TAGS ROW
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Priority Options
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PRIORITY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            priorities.forEach { p ->
                                val isSelected = selectedPriority == p.id
                                val pColor = try {
                                    Color(android.graphics.Color.parseColor(p.colorHex))
                                } catch (_: Exception) {
                                    MaterialTheme.colorScheme.primary
                                }
                                Surface(
                                    onClick = { selectedPriority = p.id },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) pColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) pColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                    )
                                ) {
                                    Text(
                                        text = p.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) pColor else MaterialTheme.colorScheme.onSurface
                                        ),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Tags Input & List
                    Column(modifier = Modifier.weight(1.1f)) {
                        Text(
                            text = "TAGS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            OutlinedTextField(
                                value = tagInput,
                                onValueChange = { tagInput = it },
                                placeholder = { Text("Add tag & Enter", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                )
                            )
                            Button(
                                onClick = {
                                    val cleaned = tagInput.trim().replace("#", "")
                                    if (cleaned.isNotBlank() && !tagsList.contains(cleaned)) {
                                        tagsList = tagsList + cleaned
                                        tagInput = ""
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("Add")
                            }
                        }

                        // Display active tags
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            tagsList.forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "#$tag",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove Tag",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clickable { tagsList = tagsList - tag }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // SUB-TASKS Section (Available for all categories!)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "SUB-TASKS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        .padding(8.dp)
                ) {
                    subtasks.forEachIndexed { index, st ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    subtasks = subtasks.toMutableList().also { list ->
                                        list[index] = st.copy(isCompleted = !st.isCompleted)
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (st.isCompleted) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                    contentDescription = "Toggle Subtask",
                                    tint = if (st.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = st.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    textDecoration = if (st.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (st.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    subtasks = subtasks.toMutableList().also { it.removeAt(index) }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete subtask",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Add new subtask row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newSubtaskTitle,
                            onValueChange = { newSubtaskTitle = it },
                            placeholder = { Text("Add sub-task...", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = {
                                if (newSubtaskTitle.isNotBlank()) {
                                    subtasks = subtasks + SubTask(
                                        id = System.currentTimeMillis().toString(),
                                        title = newSubtaskTitle.trim(),
                                        isCompleted = false
                                    )
                                    newSubtaskTitle = ""
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("+")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // DIALOG ACTION BUTTONS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (isEditMode && onDelete != null && itemToEdit != null) {
                        OutlinedButton(
                            onClick = {
                                onDelete(itemToEdit)
                                onDismiss()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("delete_item_button")
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", maxLines = 1)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("cancel_popup_button")
                        ) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    val now = System.currentTimeMillis()
                                    val newItem = ListItemEntity(
                                        id = itemToEdit?.id ?: 0L,
                                        listId = selectedListId,
                                        title = title.trim(),
                                        type = itemType,
                                        category = selectedCategory,
                                        section = section.trim(),
                                        isChecked = isChecked,
                                        checkedAt = if (isChecked) (itemToEdit?.checkedAt ?: now) else null,
                                        description = description,
                                        imageUri = attachedImageUri,
                                        createdAt = itemToEdit?.createdAt ?: now,
                                        updatedAt = now,
                                        dueDate = dueDate,
                                        priority = selectedPriority,
                                        tags = tagsList.joinToString(","),
                                        subtasksJson = SubTask.toJson(subtasks)
                                    )
                                    onSave(newItem)
                                    onDismiss()
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("save_item_button")
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEditMode) "Save Changes" else "Create Item",
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

@Composable
fun RenderItemImage(imageUri: String, modifier: Modifier = Modifier) {
    when (imageUri) {
        "sample_sticky_notes" -> {
            Image(
                painter = painterResource(id = R.drawable.sample_sticky_notes),
                contentDescription = "Sticky Notes",
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        }
        "sample_colors" -> {
            Image(
                painter = painterResource(id = R.drawable.sample_colors),
                contentDescription = "Color palette",
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        }
        else -> {
            AsyncImage(
                model = imageUri,
                contentDescription = "Item Image",
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun ItemTypeCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) accentColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                maxLines = 2
            )
        }
    }
}

@Composable
private fun QuickDateChip(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
