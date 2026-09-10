package com.example.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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

        // Default result is CANCELED if user backs out
        setResult(Activity.RESULT_CANCELED)

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // Determine default type if launched for a specific provider
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
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
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

    private fun saveAndFinish(appWidgetId: Int, config: WidgetStyleConfig) {
        val repo = ListRepository(this)
        val appWidgetManager = AppWidgetManager.getInstance(this)

        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            repo.saveWidgetConfigForAppWidgetId(appWidgetId, config)
            ListAppWidgetProvider.updateAppWidget(this, appWidgetManager, appWidgetId)

            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(Activity.RESULT_OK, resultValue)
        } else {
            repo.updateWidgetConfig(config)
            ListAppWidgetProvider.sendUpdateBroadcast(this)
            setResult(Activity.RESULT_OK)
        }
        finish()
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
    val scope = rememberCoroutineScope()

    var allLists by remember { mutableStateOf<List<ListEntity>>(emptyList()) }
    var allItems by remember { mutableStateOf<List<ListItemEntity>>(emptyList()) }
    var categories by remember { mutableStateOf<List<CategoryOption>>(emptyList()) }

    var config by remember {
        mutableStateOf(repo.getWidgetConfigForAppWidgetId(appWidgetId, defaultType))
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            allLists = db.listDao().getAllListsSync()
            allItems = db.listItemDao().getAllItemsSync()
            categories = repo.categories.value
        }
    }

    // Available tags across all items
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Configure Widget",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID)
                                "Home Screen Widget #${appWidgetId}" else "Widget Setup",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
            // Widget Type Selector Tabs
            Text(
                text = "Widget Mode",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WidgetType.values().forEach { type ->
                    val isSelected = config.widgetType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            config = config.copy(
                                widgetType = type,
                                widgetName = type.label
                            )
                        },
                        label = { Text(type.label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = {
                            val icon = when (type) {
                                WidgetType.ALL_LISTS -> Icons.Default.FolderSpecial
                                WidgetType.ALL_ITEMS -> Icons.Default.ListAlt
                                WidgetType.SPECIFIC_LIST -> Icons.Default.Checklist
                                WidgetType.SPECIFIC_ITEM -> Icons.Default.Star
                            }
                            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }

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
                                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
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
                            Text(
                                text = "Select which categories or tags appear in this widget:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Category filter chips
                            Text("Category Filter", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val categoryOptions = listOf("ALL", "TASK", "IDEA", "NOTE") + categories.map { it.name }.filter { it !in listOf("Task", "Idea", "Note") }
                                categoryOptions.forEach { cat ->
                                    val isSelected = config.categoryFilter.equals(cat, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { config = config.copy(categoryFilter = cat) },
                                        label = { Text(if (cat == "ALL") "All Categories" else cat) }
                                    )
                                }
                            }

                            // Tag filter chips
                            Text("Tag Filter", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = config.tagFilter == "ALL",
                                    onClick = { config = config.copy(tagFilter = "ALL") },
                                    label = { Text("All Tags") }
                                )
                                availableTags.forEach { tag ->
                                    val isSelected = config.tagFilter.equals(tag, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { config = config.copy(tagFilter = tag) },
                                        label = { Text("#$tag") }
                                    )
                                }
                            }

                            // Text Size Selector
                            Text("Text Size", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val sizeOptions = listOf(
                                    "SMALL" to "Small",
                                    "MEDIUM" to "Medium",
                                    "LARGE" to "Large",
                                    "XLARGE" to "X-Large"
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
                                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
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

                            // Featured Image Selector
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
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
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "All Lists Overview",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Displays all ${allLists.size} of your lists with icon, creation date, and item totals.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Theme & Gradient Style Selector
            Text(
                text = "Widget Theme & Color Style",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            val themeOptions = listOf(
                "PURPLE" to ("Purple Glow" to listOf(Color(0xFF6B21A8), Color(0xFF3B0764))),
                "WHITE" to ("Plain White" to listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9))),
                "MIDNIGHT" to ("Midnight Dark" to listOf(Color(0xFF1E293B), Color(0xFF0F172A))),
                "OCEAN" to ("Ocean Blue" to listOf(Color(0xFF0284C7), Color(0xFF0C4A6E))),
                "EMERALD" to ("Emerald Green" to listOf(Color(0xFF059669), Color(0xFF064E3B))),
                "SUNSET" to ("Sunset Amber" to listOf(Color(0xFFD97706), Color(0xFF78350F))),
                "ROSE" to ("Rose Quartz" to listOf(Color(0xFFE11D48), Color(0xFF881337))),
                "SLATE" to ("Slate Graphite" to listOf(Color(0xFF475569), Color(0xFF1E293B))),
                "MINT" to ("Mint Teal" to listOf(Color(0xFF0D9488), Color(0xFF134E4A))),
                "AMBER" to ("Warm Amber" to listOf(Color(0xFFB45309), Color(0xFF78350F))),
                "LAVENDER" to ("Soft Lavender" to listOf(Color(0xFF7E22CE), Color(0xFF581C87))),
                "CORAL" to ("Coral Red" to listOf(Color(0xFFDC2626), Color(0xFF7F1D1D)))
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(themeOptions) { (key, pair) ->
                    val (label, colors) = pair
                    val isSelected = config.themeStyle.equals(key, ignoreCase = true)
                    Surface(
                        modifier = Modifier
                            .size(width = 110.dp, height = 75.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { config = config.copy(themeStyle = key) },
                        shape = RoundedCornerShape(12.dp),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (key == "WHITE") Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF8FAFC)))
                                    else Brush.verticalGradient(colors)
                                )
                                .padding(8.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (key == "WHITE") Color(0xFF0F172A) else Color.White
                            )
                        }
                    }
                }
            }

            // Live Preview Card
            Text(
                text = "Live Widget Preview",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            WidgetLivePreviewCard(
                config = config,
                allLists = allLists,
                allItems = allItems
            )
        }
    }
}

@Composable
fun WidgetLivePreviewCard(
    config: WidgetStyleConfig,
    allLists: List<ListEntity>,
    allItems: List<ListItemEntity>
) {
    val isWhite = config.themeStyle.equals("WHITE", ignoreCase = true)
    val bgBrush = when (config.themeStyle.uppercase()) {
        "WHITE" -> Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF8FAFC)))
        "MIDNIGHT" -> Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
        "OCEAN" -> Brush.verticalGradient(listOf(Color(0xFF0284C7), Color(0xFF0C4A6E)))
        "EMERALD" -> Brush.verticalGradient(listOf(Color(0xFF059669), Color(0xFF064E3B)))
        "SUNSET" -> Brush.verticalGradient(listOf(Color(0xFFD97706), Color(0xFF78350F)))
        "ROSE" -> Brush.verticalGradient(listOf(Color(0xFFE11D48), Color(0xFF881337)))
        "SLATE" -> Brush.verticalGradient(listOf(Color(0xFF475569), Color(0xFF1E293B)))
        "MINT" -> Brush.verticalGradient(listOf(Color(0xFF0D9488), Color(0xFF134E4A)))
        "AMBER" -> Brush.verticalGradient(listOf(Color(0xFFB45309), Color(0xFF78350F)))
        "LAVENDER" -> Brush.verticalGradient(listOf(Color(0xFF7E22CE), Color(0xFF581C87)))
        "CORAL" -> Brush.verticalGradient(listOf(Color(0xFFDC2626), Color(0xFF7F1D1D)))
        else -> Brush.verticalGradient(listOf(Color(0xFF6B21A8), Color(0xFF3B0764)))
    }

    val primaryTextColor = if (isWhite) Color(0xFF0F172A) else Color.White
    val secondaryTextColor = if (isWhite) Color(0xFF64748B) else Color(0xFFCBD5E1)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 8.dp,
        border = if (isWhite) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)) else null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgBrush)
                .padding(16.dp)
        ) {
            when (config.widgetType) {
                WidgetType.SPECIFIC_ITEM -> {
                    val item = allItems.find { it.id == config.itemId } ?: allItems.firstOrNull()
                    val parentList = allLists.find { it.id == item?.listId }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (config.customImageUri != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                AsyncImage(
                                    model = config.customImageUri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = if (isWhite) Color(0xFFEEF2FF) else Color(0x33FFFFFF),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = item?.category ?: "Task",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isWhite) Color(0xFF4F46E5) else Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = parentList?.name ?: "My List",
                                style = MaterialTheme.typography.labelSmall,
                                color = secondaryTextColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = item?.title ?: "Sample Item Title",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor
                        )
                        Text(
                            text = item?.description?.ifBlank { "Full item description and notes go here." } ?: "Full item description and notes go here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryTextColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                WidgetType.ALL_LISTS -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("All Lists", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = primaryTextColor)
                            Surface(color = if (isWhite) Color(0xFFEEF2FF) else Color(0x33FFFFFF), shape = RoundedCornerShape(6.dp)) {
                                Text("+ Quick Add", style = MaterialTheme.typography.labelSmall, color = if (isWhite) Color(0xFF4F46E5) else Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Bold)
                            }
                        }
                        allLists.take(3).forEach { list ->
                            val count = allItems.count { it.listId == list.id }
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = if (isWhite) Color(0xFFF1F5F9) else Color(0x22FFFFFF),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(list.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = primaryTextColor)
                                    Text("$count items", style = MaterialTheme.typography.labelSmall, color = secondaryTextColor)
                                }
                            }
                        }
                    }
                }

                else -> {
                    // SPECIFIC_LIST or ALL_ITEMS
                    val targetList = allLists.find { it.id == config.listId } ?: allLists.firstOrNull()
                    val title = when (config.widgetType) {
                        WidgetType.ALL_ITEMS -> {
                            if (config.categoryFilter != "ALL") "All Items • ${config.categoryFilter}"
                            else if (config.tagFilter != "ALL") "All Items • #${config.tagFilter}"
                            else "All Items"
                        }
                        else -> "${targetList?.name ?: "To-Do List"} ▾"
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = primaryTextColor)
                            Surface(color = if (isWhite) Color(0xFFEEF2FF) else Color(0x33FFFFFF), shape = RoundedCornerShape(6.dp)) {
                                Text("+ Quick Add", style = MaterialTheme.typography.labelSmall, color = if (isWhite) Color(0xFF4F46E5) else Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Bold)
                            }
                        }

                        val sampleItems = if (config.widgetType == WidgetType.ALL_ITEMS) {
                            allItems.take(3)
                        } else {
                            allItems.filter { it.listId == targetList?.id }.take(3)
                        }

                        if (sampleItems.isEmpty()) {
                            Text("No items in list", style = MaterialTheme.typography.bodySmall, color = secondaryTextColor)
                        } else {
                            sampleItems.forEach { item ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = if (isWhite) Color(0xFFF1F5F9) else Color(0x22FFFFFF),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            if (item.isChecked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = if (isWhite) Color(0xFF4F46E5) else Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(item.title, style = MaterialTheme.typography.bodyMedium, color = primaryTextColor, modifier = Modifier.weight(1f))
                                        if (item.category.isNotBlank()) {
                                            Text(item.category, style = MaterialTheme.typography.labelSmall, color = secondaryTextColor)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
