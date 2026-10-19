package com.example.ui.components

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextOverflow
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
    itemTags: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (ListItemEntity) -> Unit,
    onDelete: ((ListItemEntity) -> Unit)? = null,
    onAddCustomCategory: ((name: String, iconKey: String, colorHex: String) -> Unit)? = null,
    onAddPriority: ((label: String, colorHex: String) -> Unit)? = null,
    onAddTag: ((tag: String) -> Unit)? = null
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
        mutableStateOf(if (selectedCategory == "Task") "TASK" else "NOTE")
    }
    var title by remember { mutableStateOf(itemToEdit?.title ?: "") }
    var description by remember { mutableStateOf(itemToEdit?.description ?: "") }
    var attachedImageUri by remember { mutableStateOf(itemToEdit?.imageUri) }
    var isChecked by remember { mutableStateOf(itemToEdit?.isChecked ?: false) }
    var dueDate by remember { mutableStateOf(itemToEdit?.dueDate) }
    var selectedPriority by remember { mutableStateOf(itemToEdit?.priority ?: "NONE") }
    var tagsList by remember {
        mutableStateOf(
            itemToEdit?.tags?.split(",")?.map { it.trim().removePrefix("#") }?.filter { it.isNotEmpty() } ?: emptyList()
        )
    }
    var subtasks by remember {
        mutableStateOf(SubTask.fromJson(itemToEdit?.subtasksJson ?: "[]"))
    }
    var newSubtaskTitle by remember { mutableStateOf("") }

    // Dropdowns State
    var listDropdownExpanded by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var priorityDropdownExpanded by remember { mutableStateOf(false) }
    var tagsDropdownExpanded by remember { mutableStateOf(false) }

    // Dialogs for creating new Priority or Tag from Dropdowns
    var showAddPriorityDialog by remember { mutableStateOf(false) }
    var newPriorityLabelInput by remember { mutableStateOf("") }
    var newPriorityColorInput by remember { mutableStateOf("#EF4444") }

    var showAddTagDialog by remember { mutableStateOf(false) }
    var newTagNameInput by remember { mutableStateOf("") }

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
            tonalElevation = 8.dp,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar (Icon + Title + List Dropdown + Category Dropdown + Close X)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 12.dp, top = 14.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                            .size(42.dp)
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

                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // List Selector Dropdown
                            Box {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { listDropdownExpanded = true }
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "List: ",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                    Text(
                                        text = currentList?.name ?: "Select List",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Select List",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
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

                            // Category Selector Dropdown (Unchanged Dropdown Behavior)
                            Box {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { categoryDropdownExpanded = true }
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Cat: ",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                    Text(
                                        text = selectedCategory,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = headerColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Select Category",
                                        tint = headerColor,
                                        modifier = Modifier.size(16.dp)
                                    )
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
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_popup_button")) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Scrollable Middle Form
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // New Category Creation Dialog Card (if triggered from category dropdown)
                    if (showNewCategoryInput) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
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

                    // TITLE Field
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "TITLE *",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = { Text("e.g. Review home renovation plans") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("item_title_input"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            singleLine = true
                        )
                    }

                    // DESCRIPTION & NOTES (RICH TEXT)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "DESCRIPTION & NOTES (RICH TEXT)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        RichTextEditor(
                            value = description,
                            onValueChange = { description = it }
                        )
                    }

                    // ATTACHED IMAGE Section
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "ATTACHED IMAGE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))

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
                    }

                    // CREATION DATE & DUE DATE ROW (Removed "Today/Tomorrow/Next Week" quick chips)
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
                                    color = MaterialTheme.colorScheme.primary
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

                        // Due Date (Custom Date Picker + Clear Button)
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text(
                                text = "DUE DATE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            val dueDateText = if (dueDate != null && dueDate!! > 0) {
                                SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(dueDate!!))
                            } else {
                                "Pick due date..."
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (dueDate != null) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear Due Date",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clickable { dueDate = null }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = "Pick Due Date",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // PRIORITY & TAGS DROPDOWNS ROW
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Priority Single-Select Dropdown Menu
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PRIORITY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            val currentPriorityObj = priorities.find {
                                it.id.equals(selectedPriority, ignoreCase = true) || it.label.equals(selectedPriority, ignoreCase = true)
                            }
                            val currentPriColor = remember(currentPriorityObj) {
                                try {
                                    if (currentPriorityObj != null) Color(android.graphics.Color.parseColor(currentPriorityObj.colorHex))
                                    else Color(0xFF7C3AED)
                                } catch (_: Exception) { Color(0xFF7C3AED) }
                            }

                            Box {
                                Surface(
                                    onClick = { priorityDropdownExpanded = true },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = BorderStroke(1.dp, currentPriColor.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = currentPriorityObj?.label ?: selectedPriority,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = currentPriColor
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = currentPriColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = priorityDropdownExpanded,
                                    onDismissRequest = { priorityDropdownExpanded = false }
                                ) {
                                    priorities.forEach { p ->
                                        val pColor = try {
                                            Color(android.graphics.Color.parseColor(p.colorHex))
                                        } catch (_: Exception) { MaterialTheme.colorScheme.primary }

                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .clip(CircleShape)
                                                            .background(pColor)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(p.label, fontWeight = if (selectedPriority == p.id) FontWeight.Bold else FontWeight.Normal)
                                                }
                                            },
                                            onClick = {
                                                selectedPriority = p.id
                                                priorityDropdownExpanded = false
                                            }
                                        )
                                    }

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("+ Create Priority Tag", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                        },
                                        onClick = {
                                            priorityDropdownExpanded = false
                                            newPriorityLabelInput = ""
                                            newPriorityColorInput = "#EF4444"
                                            showAddPriorityDialog = true
                                        }
                                    )
                                }
                            }
                        }

                        // Tags Multi-Select Dropdown Menu
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text(
                                text = "TAGS (${tagsList.size})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            val tagsSummaryText = if (tagsList.isEmpty()) "Select tags..." else tagsList.joinToString(", ") { "#$it" }

                            Box {
                                Surface(
                                    onClick = { tagsDropdownExpanded = true },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = tagsSummaryText,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = if (tagsList.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                                                fontWeight = if (tagsList.isEmpty()) FontWeight.Normal else FontWeight.SemiBold
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = tagsDropdownExpanded,
                                    onDismissRequest = { tagsDropdownExpanded = false }
                                ) {
                                    val availableTags = (itemTags.map { it.removePrefix("#") } + tagsList).distinct().sorted()

                                    if (availableTags.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("No tags available", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                            onClick = { }
                                        )
                                    } else {
                                        availableTags.forEach { tag ->
                                            val isSelected = tag in tagsList
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Checkbox(
                                                            checked = isSelected,
                                                            onCheckedChange = {
                                                                tagsList = if (isSelected) tagsList - tag else tagsList + tag
                                                            }
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("#$tag")
                                                    }
                                                },
                                                onClick = {
                                                    tagsList = if (isSelected) tagsList - tag else tagsList + tag
                                                }
                                            )
                                        }
                                    }

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("+ Create Item Tag", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                        },
                                        onClick = {
                                            tagsDropdownExpanded = false
                                            newTagNameInput = ""
                                            showAddTagDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // SUB-TASKS Section (Available for all items)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "SUB-TASKS (${subtasks.count { it.isCompleted }}/${subtasks.size})",
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
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
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
                    }
                }

                // FIXED ACTION BUTTONS ALONG BOTTOM (Delete, Cancel, Save Changes / Create Item - ALWAYS VISIBLE!)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
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
                                        section = itemToEdit?.section ?: "",
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

    // --- DIALOG FOR CREATING NEW PRIORITY TAG FROM DROPDOWN ---
    if (showAddPriorityDialog) {
        AlertDialog(
            onDismissRequest = { showAddPriorityDialog = false },
            title = { Text("Create Priority Tag", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newPriorityLabelInput,
                        onValueChange = { newPriorityLabelInput = it },
                        label = { Text("Priority Label") },
                        placeholder = { Text("e.g. Urgent, Critical, Soon...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPriorityLabelInput.isNotBlank()) {
                            val labelTrim = newPriorityLabelInput.trim()
                            onAddPriority?.invoke(labelTrim, newPriorityColorInput)
                            selectedPriority = labelTrim
                            showAddPriorityDialog = false
                            newPriorityLabelInput = ""
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Create Priority")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showAddPriorityDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- DIALOG FOR CREATING NEW ITEM TAG FROM DROPDOWN ---
    if (showAddTagDialog) {
        AlertDialog(
            onDismissRequest = { showAddTagDialog = false },
            title = { Text("Create Item Tag", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newTagNameInput,
                    onValueChange = { newTagNameInput = it },
                    label = { Text("Tag Name") },
                    placeholder = { Text("e.g. work, home, idea...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTagNameInput.isNotBlank()) {
                            val cleanTag = newTagNameInput.trim().replace("#", "")
                            onAddTag?.invoke(cleanTag)
                            if (!tagsList.contains(cleanTag)) {
                                tagsList = tagsList + cleanTag
                            }
                            showAddTagDialog = false
                            newTagNameInput = ""
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Create Tag")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showAddTagDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
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
