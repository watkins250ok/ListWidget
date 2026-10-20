package com.example.widget

import android.R
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.ListRepository
import com.example.ui.theme.ListWidgetAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(R.color.transparent)
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)

        setResult(RESULT_CANCELED)

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val appWidgetManager = AppWidgetManager.getInstance(this)
        val providerInfo = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            appWidgetManager?.getAppWidgetInfo(appWidgetId)
        } else null

        val defaultType = when (providerInfo?.provider?.className) {
            AllListsWidgetProvider::class.java.name -> WidgetType.ALL_LISTS
            AllItemsWidgetProvider::class.java.name -> WidgetType.ALL_ITEMS
            SpecificItemWidgetProvider::class.java.name -> WidgetType.SPECIFIC_ITEM
            SpecificListWidgetProvider::class.java.name -> WidgetType.SPECIFIC_LIST
            else -> WidgetType.SPECIFIC_LIST
        }

        setContent {
            ListWidgetAppTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { finish() },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .fillMaxHeight(0.85f)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable(enabled = false) {},
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        WidgetConfigScreen(
                            appWidgetId = appWidgetId,
                            defaultType = defaultType,
                            onConfigSaved = { config ->
                                saveAndFinish(appWidgetId, config)
                            },
                            onCancel = {
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun saveAndFinish(appWidgetId: Int, config: WidgetStyleConfig) {
        val repo = ListRepository(this)
        val appWidgetManager = AppWidgetManager.getInstance(this)

        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            repo.saveWidgetConfigForAppWidgetId(appWidgetId, config)
            ListAppWidgetProvider.updateAppWidget(this, appWidgetManager, appWidgetId)

            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(RESULT_OK, resultValue)
        } else {
            repo.updateWidgetConfig(config)
            ListAppWidgetProvider.sendUpdateBroadcast(this)
            setResult(RESULT_OK)
        }
        finish()
    }

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigScreen(
    appWidgetId: Int,
    defaultType: WidgetType,
    onConfigSaved: (WidgetStyleConfig) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val repo = remember { ListRepository(context) }

    var allLists by remember { mutableStateOf<List<ListEntity>>(emptyList()) }
    var allItems by remember { mutableStateOf<List<ListItemEntity>>(emptyList()) }
    var categories by remember { mutableStateOf<List<CategoryOption>>(emptyList()) }

    var config by remember {
        mutableStateOf(repo.getWidgetConfigForAppWidgetId(appWidgetId, defaultType))
    }

    // Dropdown visibility states
    var colorDropdownExpanded by remember { mutableStateOf(false) }
    var catMenuExpanded by remember { mutableStateOf(false) }
    var tagMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            allLists = db.listDao().getAllListsSync()
            allItems = db.listItemDao().getAllItemsSync()
            categories = repo.categories.value
        }
    }

    val availableTags = remember(allItems) {
        allItems.flatMap { item ->
            item.tags.split(",", " ", "#")
                .map { it.trim().removePrefix("#") }
                .filter { it.isNotBlank() }
        }.distinct().sorted()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Throwable) {}
            config = config.copy(customImageUri = uri.toString())
        }
    }

    var itemSearchQuery by remember { mutableStateOf("") }

    val themeColorOptions = listOf(
        Triple("PURPLE", "Purple Glow", Color(0xFF7C3AED)),
        Triple("WHITE", "Plain White", Color(0xFFFFFFFF)),
        Triple("MIDNIGHT", "Midnight Dark", Color(0xFF0F172A)),
        Triple("OCEAN", "Ocean Blue", Color(0xFF0284C7)),
        Triple("EMERALD", "Emerald Green", Color(0xFF059669)),
        Triple("SUNSET", "Sunset Amber", Color(0xFFEA580C)),
        Triple("ROSE", "Rose Quartz", Color(0xFFDB2777)),
        Triple("SLATE", "Slate Graphite", Color(0xFF334155)),
        Triple("MINT", "Mint Teal", Color(0xFF0D9488)),
        Triple("AMBER", "Warm Amber", Color(0xFFD97706)),
        Triple("LAVENDER", "Soft Lavender", Color(0xFF6366F1)),
        Triple("CORAL", "Coral Red", Color(0xFFE11D48))
    )

    val selectedColorObj = themeColorOptions.find { it.first.equals(config.themeStyle, ignoreCase = true) } ?: themeColorOptions.first()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Configure Widget",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onConfigSaved(config) },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apply & Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Type-Specific Options
            when (config.widgetType) {
                WidgetType.SPECIFIC_LIST -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Checklist, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Select List to Display",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Pick the target list that will be visible on your home screen:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (allLists.isEmpty()) {
                                Text("No lists available. Default list will be used.", style = MaterialTheme.typography.bodyMedium)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    allLists.forEach { list ->
                                        val isSelected = (config.listId == list.id) || (config.listId == 0L && list == allLists.first())
                                        val itemCount = allItems.count { it.listId == list.id }
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { config = config.copy(listId = list.id) },
                                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = { config = config.copy(listId = list.id) }
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = list.name,
                                                        fontWeight = FontWeight.SemiBold,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    if (!list.description.isNullOrBlank()) {
                                                        Text(
                                                            text = list.description.orEmpty(),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(
                                                        text = "$itemCount items",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Text Size Selector (Small, Medium, Large)
                            Text("Text Size", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val sizeOptions = listOf(
                                    "SMALL" to "Small",
                                    "MEDIUM" to "Medium",
                                    "LARGE" to "Large"
                                )
                                sizeOptions.forEach { (key, label) ->
                                    val isSelected = config.textSizeScale.equals(key, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { config = config.copy(textSizeScale = key) },
                                        label = { Text(label) }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Hide checked tasks", style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = config.hideChecked,
                                    onCheckedChange = { config = config.copy(hideChecked = it) }
                                )
                            }
                        }
                    }
                }

                WidgetType.ALL_ITEMS -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FilterList, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Category & Tag Filters",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Category Filter Multi-Select Dropdown Menu
                            val selectedCatList = remember(config.categoryFilter) {
                                if (config.categoryFilter == "ALL") emptyList()
                                else config.categoryFilter.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            }
                            val catSummary = if (selectedCatList.isEmpty()) "All Categories" else "${selectedCatList.size} Selected (${selectedCatList.joinToString(", ")})"

                            Text("Category Filter", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Surface(
                                    onClick = { catMenuExpanded = true },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(catSummary, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                }

                                DropdownMenu(
                                    expanded = catMenuExpanded,
                                    onDismissRequest = { catMenuExpanded = false }
                                ) {
                                    val allCatNames = (listOf("Task", "Idea", "Note") + categories.map { it.name }).distinct()
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(checked = selectedCatList.isEmpty(), onCheckedChange = { config = config.copy(categoryFilter = "ALL") })
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("All Categories", fontWeight = FontWeight.Bold)
                                            }
                                        },
                                        onClick = { config = config.copy(categoryFilter = "ALL") }
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                                    allCatNames.forEach { cat ->
                                        val isSel = cat in selectedCatList
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Checkbox(
                                                        checked = isSel,
                                                        onCheckedChange = {
                                                            val next = if (isSel) selectedCatList - cat else selectedCatList + cat
                                                            config = config.copy(categoryFilter = if (next.isEmpty()) "ALL" else next.joinToString(","))
                                                        }
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(cat)
                                                }
                                            },
                                            onClick = {
                                                val next = if (isSel) selectedCatList - cat else selectedCatList + cat
                                                config = config.copy(categoryFilter = if (next.isEmpty()) "ALL" else next.joinToString(","))
                                            }
                                        )
                                    }
                                }
                            }

                            // Tag Filter Multi-Select Dropdown Menu
                            val selectedTagList = remember(config.tagFilter) {
                                if (config.tagFilter == "ALL") emptyList()
                                else config.tagFilter.split(",").map { it.trim().removePrefix("#") }.filter { it.isNotBlank() }
                            }
                            val tagSummary = if (selectedTagList.isEmpty()) "All Tags" else "${selectedTagList.size} Selected (${selectedTagList.joinToString(", ") { "#$it" }})"

                            Text("Tag Filter", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Surface(
                                    onClick = { tagMenuExpanded = true },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(tagSummary, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                }

                                DropdownMenu(
                                    expanded = tagMenuExpanded,
                                    onDismissRequest = { tagMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(checked = selectedTagList.isEmpty(), onCheckedChange = { config = config.copy(tagFilter = "ALL") })
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("All Tags", fontWeight = FontWeight.Bold)
                                            }
                                        },
                                        onClick = { config = config.copy(tagFilter = "ALL") }
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                                    availableTags.forEach { tag ->
                                        val isSel = tag in selectedTagList
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Checkbox(
                                                        checked = isSel,
                                                        onCheckedChange = {
                                                            val next = if (isSel) selectedTagList - tag else selectedTagList + tag
                                                            config = config.copy(tagFilter = if (next.isEmpty()) "ALL" else next.joinToString(","))
                                                        }
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("#$tag")
                                                }
                                            },
                                            onClick = {
                                                val next = if (isSel) selectedTagList - tag else selectedTagList + tag
                                                config = config.copy(tagFilter = if (next.isEmpty()) "ALL" else next.joinToString(","))
                                            }
                                        )
                                    }
                                }
                            }

                            // Text Size Selector (Small, Medium, Large)
                            Text("Text Size", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val sizeOptions = listOf(
                                    "SMALL" to "Small",
                                    "MEDIUM" to "Medium",
                                    "LARGE" to "Large"
                                )
                                sizeOptions.forEach { (key, label) ->
                                    val isSelected = config.textSizeScale.equals(key, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { config = config.copy(textSizeScale = key) },
                                        label = { Text(label) }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Hide checked tasks", style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = config.hideChecked,
                                    onCheckedChange = { config = config.copy(hideChecked = it) }
                                )
                            }
                        }
                    }
                }

                WidgetType.SPECIFIC_ITEM -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Select Item to Feature",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Choose a specific task or note to display prominently on your home screen:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = itemSearchQuery,
                                onValueChange = { itemSearchQuery = it },
                                placeholder = { Text("Search items...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            val filteredItems = remember(allItems, itemSearchQuery) {
                                if (itemSearchQuery.isBlank()) allItems
                                else allItems.filter {
                                    it.title.contains(itemSearchQuery, ignoreCase = true) ||
                                            it.description.contains(itemSearchQuery, ignoreCase = true)
                                }
                            }

                            if (filteredItems.isEmpty()) {
                                Text("No items found.", style = MaterialTheme.typography.bodyMedium)
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 260.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    filteredItems.forEach { item ->
                                        val isSelected = config.itemId == item.id || (config.itemId == 0L && item == filteredItems.first())
                                        val parentList = allLists.find { it.id == item.listId }
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { config = config.copy(itemId = item.id) },
                                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = { config = config.copy(itemId = item.id) }
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.title,
                                                        fontWeight = FontWeight.SemiBold,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = parentList?.name ?: "List",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Text("•", style = MaterialTheme.typography.labelSmall)
                                                        Text(
                                                            text = item.category,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Text Size Selector (Small, Medium, Large)
                            Text("Text Size", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val sizeOptions = listOf(
                                    "SMALL" to "Small",
                                    "MEDIUM" to "Medium",
                                    "LARGE" to "Large"
                                )
                                sizeOptions.forEach { (key, label) ->
                                    val isSelected = config.textSizeScale.equals(key, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { config = config.copy(textSizeScale = key) },
                                        label = { Text(label) }
                                    )
                                }
                            }

                            // Featured Image Selector
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                text = "Hero Photo Banner (Optional)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (config.customImageUri != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                    ) {
                                        AsyncImage(
                                            model = config.customImageUri,
                                            contentDescription = "Selected Photo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                photoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            }
                                        ) {
                                            Text("Change Photo")
                                        }
                                        TextButton(onClick = { config = config.copy(customImageUri = null) }) {
                                            Text("Remove Photo (Text Only)")
                                        }
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                    ) {
                                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Add Photo to Card")
                                    }
                                }
                            }
                        }
                    }
                }

                WidgetType.ALL_LISTS -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Text Size Selector (Small, Medium, Large)
                            Text("Text Size", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val sizeOptions = listOf(
                                    "SMALL" to "Small",
                                    "MEDIUM" to "Medium",
                                    "LARGE" to "Large"
                                )
                                sizeOptions.forEach { (key, label) ->
                                    val isSelected = config.textSizeScale.equals(key, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { config = config.copy(textSizeScale = key) },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Theme & Color Style Selector Dropdown with Swatch and Name
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    onClick = { colorDropdownExpanded = true },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(selectedColorObj.third)
                                    .border(1.dp, if (selectedColorObj.first == "WHITE") Color.Gray else Color.Transparent, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = selectedColorObj.second,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Color Style",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = colorDropdownExpanded,
                    onDismissRequest = { colorDropdownExpanded = false }
                ) {
                    themeColorOptions.forEach { (key, label, color) ->
                        val isSelected = config.themeStyle.equals(key, ignoreCase = true)
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(1.dp, if (key == "WHITE") Color.Gray else Color.Transparent, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            },
                            onClick = {
                                config = config.copy(themeStyle = key)
                                colorDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
