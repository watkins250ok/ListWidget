package com.example.ui.screens

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RestorePage
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.data.model.AppFontFamily
import com.example.data.model.AppThemeMode
import com.example.data.model.CategoryOption
import com.example.data.model.ColorPalettePreset
import com.example.data.model.ListEntity
import com.example.data.model.ListItemEntity
import com.example.data.model.PriorityOption
import com.example.data.model.WidgetStyleConfig
import com.example.data.model.WidgetType
import com.example.ui.components.IconPickerGrid
import com.example.ui.components.getVectorForCategory
import com.example.widget.ListAppWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AppUpdateResult(
    val isUpdateAvailable: Boolean,
    val latestTag: String,
    val currentVersion: String,
    val releaseNotes: String,
    val releaseUrl: String,
    val apkUrl: String?
)

private suspend fun fetchLatestGitHubRelease(context: Context): AppUpdateResult = withContext(Dispatchers.IO) {
    val currentVersion = try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        pInfo.versionName ?: "1.0.0"
    } catch (_: Throwable) { "1.0.0" }

    val url = URL("https://api.github.com/repos/watkins250ok/ListWidgets/releases/latest")
    val connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        setRequestProperty("Accept", "application/vnd.github+json")
        setRequestProperty("User-Agent", "ListWidget-AndroidApp")
        connectTimeout = 8000
        readTimeout = 8000
    }

    if (connection.responseCode == 200) {
        val responseText = connection.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(responseText)

        val latestTagRaw = json.optString("tag_name", "")
        val latestTagClean = latestTagRaw.removePrefix("v").trim()
        val releaseUrl = json.optString("html_url", "https://github.com/watkins250ok/ListWidgets/releases")
        val body = json.optString("body", "No release notes provided.")

        var apkUrl: String? = null
        val assets = json.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val assetName = asset.optString("name", "")
                if (assetName.endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.optString("browser_download_url", null)
                    break
                }
            }
        }

        val isNewer = compareVersions(latestTagClean, currentVersion) > 0

        AppUpdateResult(
            isUpdateAvailable = isNewer,
            latestTag = latestTagRaw.ifBlank { "v$latestTagClean" },
            currentVersion = currentVersion,
            releaseNotes = body,
            releaseUrl = releaseUrl,
            apkUrl = apkUrl
        )
    } else {
        throw IOException("GitHub API returned HTTP ${connection.responseCode}")
    }
}

private fun compareVersions(latest: String, current: String): Int {
    val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
    val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
    val maxLen = maxOf(latestParts.size, currentParts.size)

    for (i in 0 until maxLen) {
        val l = latestParts.getOrElse(i) { 0 }
        val c = currentParts.getOrElse(i) { 0 }
        if (l > c) return 1
        if (c > l) return -1
    }
    return 0
}

private fun sendFeedbackEmail(context: Context, messageText: String) {
    val currentVersion = try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        pInfo.versionName ?: "1.0.0"
    } catch (_: Throwable) { "1.0.0" }

    val recipient = "watkins250ok@gmail.com"
    val subject = "ListWidget Feedback (v$currentVersion)"
    val body = StringBuilder().apply {
        if (messageText.isNotBlank()) {
            append(messageText)
            append("\n\n")
        }
        append("--- App & Device Info ---\n")
        append("App Version: ").append(currentVersion).append("\n")
        append("Android Version: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n")
        append("Device: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL)
    }.toString()

    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }

    try {
        val chooser = Intent.createChooser(intent, "Send Feedback via Email")
        context.startActivity(chooser)
    } catch (_: Throwable) {
        Toast.makeText(context, "No email client found on device", Toast.LENGTH_SHORT).show()
    }
}

data class WidgetThemeStyleOption(
    val key: String,
    val label: String,
    val previewColor: Color,
    val gradientColors: List<Color>,
    val isLight: Boolean = false
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WidgetStudioScreen(
    lists: List<ListEntity>,
    allItems: List<ListItemEntity>,
    widgetConfig: WidgetStyleConfig,
    allWidgets: List<WidgetStyleConfig> = emptyList(),
    activeWidgetId: String = "widget_1",
    onSelectActiveWidget: (String) -> Unit = {},
    onAddNewWidget: (String?, WidgetType) -> Unit = { _, _ -> },
    onDuplicateWidget: (String) -> Unit = {},
    onDeleteWidget: (String) -> Unit = {},
    themeMode: AppThemeMode,
    colorPalette: ColorPalettePreset,
    fontFamily: AppFontFamily,
    showCounts: Boolean,
    priorities: List<PriorityOption>,
    categories: List<CategoryOption> = CategoryOption.DEFAULT_CATEGORIES,
    onUpdateWidgetConfig: (WidgetStyleConfig) -> Unit,
    onSetThemeMode: (AppThemeMode) -> Unit,
    onSetColorPalette: (ColorPalettePreset) -> Unit,
    onSetFontFamily: (AppFontFamily) -> Unit,
    onSetShowCounts: (Boolean) -> Unit,
    onExportBackup: suspend () -> String = { "" },
    onRestoreBackup: suspend (String) -> Boolean = { false },
    onAddPriority: (String, String) -> Unit,
    onUpdatePriority: (String, String, String) -> Unit = { _, _, _ -> },
    onDeletePriority: (String) -> Unit = {},
    onResetPriorities: () -> Unit = {},
    onAddCategory: (String, String, String) -> Unit = { _, _, _ -> },
    onUpdateCategory: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    onDeleteCategory: (String) -> Unit = {},
    onResetCategories: () -> Unit = {},
    onToggleTaskChecked: (Long, Boolean) -> Unit,
    onQuickAddClick: (Long) -> Unit,
    onClickItem: (ListItemEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var listDropdownOpen by remember { mutableStateOf(false) }
    var itemPickerDropdownOpen by remember { mutableStateOf(false) }

    // Multi-Widget Dialogs
    var showAddWidgetDialog by remember { mutableStateOf(false) }
    var newWidgetNameInput by remember { mutableStateOf("") }
    var newWidgetTypeSelected by remember { mutableStateOf(WidgetType.SPECIFIC_LIST) }

    var showRenameWidgetDialog by remember { mutableStateOf(false) }
    var renameWidgetInput by remember { mutableStateOf("") }

    val activeList = remember(lists, widgetConfig.listId) {
        lists.find { it.id == widgetConfig.listId } ?: lists.firstOrNull()
    }

    val featuredItem = remember(allItems, widgetConfig.itemId) {
        allItems.find { it.id == widgetConfig.itemId } ?: allItems.firstOrNull()
    }

    // Filtered items based on active widget type & settings
    val displayedItems = remember(allItems, activeList, widgetConfig) {
        when (widgetConfig.widgetType) {
            WidgetType.ALL_LISTS -> emptyList()
            WidgetType.ALL_ITEMS -> {
                var list = allItems
                if (widgetConfig.categoryFilter != "ALL") {
                    list = list.filter { it.category.equals(widgetConfig.categoryFilter, ignoreCase = true) }
                }
                if (widgetConfig.tagFilter != "ALL") {
                    val tag = widgetConfig.tagFilter.removePrefix("#").trim()
                    list = list.filter { it.tags.contains(tag, ignoreCase = true) }
                }
                if (widgetConfig.hideChecked) {
                    list = list.filter { !it.isChecked }
                }
                list
            }
            WidgetType.SPECIFIC_LIST -> {
                val list = if (activeList != null) allItems.filter { it.listId == activeList.id } else allItems
                if (widgetConfig.hideChecked) list.filter { !it.isChecked } else list
            }
            WidgetType.SPECIFIC_ITEM -> {
                if (featuredItem != null) listOf(featuredItem) else emptyList()
            }
        }
    }

    // Photo picker for featured item main image
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            onUpdateWidgetConfig(widgetConfig.copy(customImageUri = uri.toString()))
        }
    }

    var newPriorityLabel by remember { mutableStateOf("") }
    var isCheckingUpdates by remember { mutableStateOf(false) }
    var updateResultDialogData by remember { mutableStateOf<AppUpdateResult?>(null) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var feedbackInput by remember { mutableStateOf("") }
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }

    val createJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val jsonStr = onExportBackup()
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(jsonStr.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(context, "Backup exported successfully!", Toast.LENGTH_LONG).show()
                } catch (_: Throwable) {
                    Toast.makeText(context, "Failed to export backup file.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val openJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
                if (!content.isNullOrBlank()) {
                    pendingRestoreJson = content
                } else {
                    Toast.makeText(context, "Selected backup file is empty.", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Throwable) {
                Toast.makeText(context, "Failed to read backup file.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var newPriorityColor by remember { mutableStateOf("#EC4899") }

    // Priority Edit & Delete Dialog State
    var priorityToEdit by remember { mutableStateOf<PriorityOption?>(null) }
    var editPriorityLabelInput by remember { mutableStateOf("") }
    var editPriorityColorInput by remember { mutableStateOf("#EF4444") }
    var priorityToDelete by remember { mutableStateOf<PriorityOption?>(null) }

    // Category Management state
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryNameInput by remember { mutableStateOf("") }
    var newCategoryIconInput by remember { mutableStateOf("tag") }
    var newCategoryColorInput by remember { mutableStateOf("#0284C7") }

    var categoryToEdit by remember { mutableStateOf<CategoryOption?>(null) }
    var editCategoryNameInput by remember { mutableStateOf("") }
    var editCategoryIconInput by remember { mutableStateOf("tag") }
    var editCategoryColorInput by remember { mutableStateOf("#0284C7") }

    var categoryToDelete by remember { mutableStateOf<CategoryOption?>(null) }

    val priorityColorPresets = listOf(
        "#64748B", "#3B82F6", "#0284C7", "#10B981",
        "#14B8A6", "#EAB308", "#F59E0B", "#F97316",
        "#EF4444", "#EC4899", "#8B5CF6", "#6366F1"
    )

    val categoryColorPresets = listOf(
        "#6366F1", "#F59E0B", "#EC4899", "#10B981",
        "#3B82F6", "#8B5CF6", "#0284C7", "#EF4444", "#14B8A6", "#F97316"
    )

    val widgetThemeOptions = listOf(
        WidgetThemeStyleOption("PURPLE", "Purple Glow", Color(0xFF7C3AED), listOf(Color(0xFF6D28D9), Color(0xFF3B0764))),
        WidgetThemeStyleOption("WHITE", "Plain White", Color(0xFFFFFFFF), listOf(Color(0xFFFFFFFF), Color(0xFFF8FAFC)), isLight = true),
        WidgetThemeStyleOption("MIDNIGHT", "Midnight Dark", Color(0xFF0F172A), listOf(Color(0xFF0F172A), Color(0xFF020617))),
        WidgetThemeStyleOption("OCEAN", "Ocean Blue", Color(0xFF0284C7), listOf(Color(0xFF0284C7), Color(0xFF082F49))),
        WidgetThemeStyleOption("EMERALD", "Emerald Green", Color(0xFF059669), listOf(Color(0xFF059669), Color(0xFF064E3B))),
        WidgetThemeStyleOption("SUNSET", "Sunset Amber", Color(0xFFEA580C), listOf(Color(0xFFEA580C), Color(0xFF7C2D12))),
        WidgetThemeStyleOption("ROSE", "Rose Quartz", Color(0xFFDB2777), listOf(Color(0xFFDB2777), Color(0xFF700736))),
        WidgetThemeStyleOption("SLATE", "Slate Graphite", Color(0xFF334155), listOf(Color(0xFF334155), Color(0xFF0F172A))),
        WidgetThemeStyleOption("MINT", "Mint Teal", Color(0xFF0D9488), listOf(Color(0xFF0D9488), Color(0xFF042F2E))),
        WidgetThemeStyleOption("AMBER", "Warm Amber", Color(0xFFD97706), listOf(Color(0xFFD97706), Color(0xFF78350F))),
        WidgetThemeStyleOption("LAVENDER", "Soft Lavender", Color(0xFF6366F1), listOf(Color(0xFF6366F1), Color(0xFF312E81))),
        WidgetThemeStyleOption("CORAL", "Coral Red", Color(0xFFE11D48), listOf(Color(0xFFE11D48), Color(0xFF881337)))
    )

    val currentThemeOption = widgetThemeOptions.find { it.key.equals(widgetConfig.themeStyle, ignoreCase = true) }
        ?: widgetThemeOptions.first()
    val isWhiteWidget = currentThemeOption.key == "WHITE"
    val widgetBgBrush = remember(widgetConfig.themeStyle) {
        Brush.verticalGradient(currentThemeOption.gradientColors)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("widget_studio_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Studio Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Settings & Customization",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Create multiple widgets, choose widget types & styling",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        }

        // Section: Multi-Widget Switcher
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Your Home Screen Widgets",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Add and configure independent widgets with individual settings",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Horizontal Scrollable Widget Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        allWidgets.forEach { w ->
                            val isSelected = w.widgetId == activeWidgetId
                            val themeOpt = widgetThemeOptions.find { it.key.equals(w.themeStyle, ignoreCase = true) } ?: widgetThemeOptions.first()

                            Surface(
                                onClick = { onSelectActiveWidget(w.widgetId) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(themeOpt.previewColor)
                                            .border(1.dp, if (themeOpt.isLight) Color(0xFFCBD5E1) else Color.Transparent, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = w.widgetName,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                        Text(
                                            text = w.widgetType.label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Add New Widget Button
                        OutlinedButton(
                            onClick = {
                                newWidgetNameInput = "Widget ${allWidgets.size + 1}"
                                showAddWidgetDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Widget", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Active Widget Action Toolbar (Rename, Duplicate, Delete)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Editing: ${widgetConfig.widgetName}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = {
                                    renameWidgetInput = widgetConfig.widgetName
                                    showRenameWidgetDialog = true
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Rename Widget", modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { onDuplicateWidget(widgetConfig.widgetId) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate Widget", modifier = Modifier.size(16.dp))
                            }
                            if (allWidgets.size > 1) {
                                IconButton(
                                    onClick = { onDeleteWidget(widgetConfig.widgetId) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Widget", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: 4 Widget Options
        item {
            Text(
                text = "WIDGET DISPLAY TYPE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val widgetTypes = listOf(
                    Triple(
                        WidgetType.ALL_LISTS,
                        "1. All Lists",
                        "Displays name, icon, description & creation date of all created lists"
                    ),
                    Triple(
                        WidgetType.ALL_ITEMS,
                        "2. All Items",
                        "Displays all items across lists, with options to filter by category or tag"
                    ),
                    Triple(
                        WidgetType.SPECIFIC_LIST,
                        "3. Specific List",
                        "Displays items from selected list with an interactive on-widget dropdown menu"
                    ),
                    Triple(
                        WidgetType.SPECIFIC_ITEM,
                        "4. Specific Item",
                        "Displays all details of a specific item, with option to feature a main image"
                    )
                )

                widgetTypes.forEach { (type, title, subtitle) ->
                    val isSelected = widgetConfig.widgetType == type
                    Surface(
                        onClick = { onUpdateWidgetConfig(widgetConfig.copy(widgetType = type)) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                val icon = when (type) {
                                    WidgetType.ALL_LISTS -> Icons.Default.ViewAgenda
                                    WidgetType.ALL_ITEMS -> Icons.Default.Checklist
                                    WidgetType.SPECIFIC_LIST -> Icons.Default.List
                                    WidgetType.SPECIFIC_ITEM -> Icons.Default.Star
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                )
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Live Interactive Widget Preview / Simulator
        item {
            Text(
                text = "LIVE HOMESCREEN SIMULATOR",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Simulated Widget Frame
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .testTag("widget_preview_card"),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = androidx.compose.foundation.BorderStroke(
                    2.dp,
                    if (isWhiteWidget) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.2f)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(widgetBgBrush)
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // 1. SIMULATOR FOR "ALL LISTS"
                        if (widgetConfig.widgetType == WidgetType.ALL_LISTS) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "All Lists",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isWhiteWidget) Color(0xFF0F172A) else Color.White
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isWhiteWidget) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "${lists.size} Lists",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isWhiteWidget) Color(0xFF334155) else Color.White,
                                                fontSize = 10.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Surface(
                                    onClick = { onQuickAddClick(0L) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isWhiteWidget) Color(0xFF4F46E5) else Color.White.copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = "+ New List",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (lists.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No lists created yet.",
                                        color = if (isWhiteWidget) Color(0xFF64748B) else Color.White.copy(alpha = 0.7f),
                                        fontSize = 13.sp
                                    )
                                }
                            } else {
                                lists.forEach { listEntity ->
                                    val itemCount = allItems.count { it.listId == listEntity.id }
                                    val iconEmoji = when (listEntity.iconName) {
                                        "check", "checklist" -> "✅"
                                        "shopping" -> "🛒"
                                        "work" -> "💼"
                                        "idea" -> "💡"
                                        "star" -> "⭐"
                                        "folder" -> "📁"
                                        "book", "bookmark" -> "📖"
                                        "favorite", "heart" -> "❤️"
                                        "plane" -> "✈️"
                                        "fitness" -> "🏋️"
                                        else -> "📋"
                                    }

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isWhiteWidget) Color(0xFFF8FAFC) else Color.White.copy(alpha = 0.15f)
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isWhiteWidget) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.1f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = iconEmoji, fontSize = 20.sp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = listEntity.name,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isWhiteWidget) Color(0xFF0F172A) else Color.White
                                                    ),
                                                    maxLines = 1
                                                )
                                                if (!listEntity.description.isNullOrBlank()) {
                                                    Text(
                                                        text = listEntity.description,
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontSize = 11.sp,
                                                            color = if (isWhiteWidget) Color(0xFF475569) else Color.White.copy(alpha = 0.8f)
                                                        ),
                                                        maxLines = 1
                                                    )
                                                }
                                                Text(
                                                    text = "📅 Created " + SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(listEntity.createdAt)),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 9.sp,
                                                        color = if (isWhiteWidget) Color(0xFF64748B) else Color.White.copy(alpha = 0.65f)
                                                    )
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isWhiteWidget) Color(0xFFEEF2F6) else Color.White.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "$itemCount items",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = if (isWhiteWidget) Color(0xFF334155) else Color.White,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. SIMULATOR FOR "SPECIFIC ITEM"
                        else if (widgetConfig.widgetType == WidgetType.SPECIFIC_ITEM) {
                            if (featuredItem == null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No item selected.\nPlease choose an item to feature below.",
                                        color = if (isWhiteWidget) Color(0xFF64748B) else Color.White.copy(alpha = 0.7f),
                                        fontSize = 13.sp
                                    )
                                }
                            } else {
                                val itemParentList = lists.find { it.id == featuredItem.listId }
                                val imageToShow = widgetConfig.customImageUri ?: featuredItem.imageUri

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Main Image: If present, show hero image. If no image selected, goes straight to description and details!
                                    if (!imageToShow.isNullOrBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(130.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                        ) {
                                            Image(
                                                painter = rememberAsyncImagePainter(imageToShow),
                                                contentDescription = "Widget Main Image",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }

                                    // Item Title & Badges
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (featuredItem.type == "TASK") {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        if (featuredItem.isChecked) Color(0xFF10B981)
                                                        else (if (isWhiteWidget) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.2f))
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (featuredItem.isChecked) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }

                                        Text(
                                            text = featuredItem.title,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isWhiteWidget) Color(0xFF0F172A) else Color.White
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Category, Priority, and List Badges
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isWhiteWidget) Color(0xFFEEF2F6) else Color.White.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = itemParentList?.name ?: "All Items",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (isWhiteWidget) Color(0xFF334155) else Color.White,
                                                    fontSize = 10.sp
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isWhiteWidget) Color(0xFFE0E7FF) else Color(0xFF6366F1).copy(alpha = 0.4f)
                                        ) {
                                            Text(
                                                text = featuredItem.category,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (isWhiteWidget) Color(0xFF4338CA) else Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isWhiteWidget) Color(0xFFFEE2E2) else Color(0xFFEF4444).copy(alpha = 0.4f)
                                        ) {
                                            Text(
                                                text = "${featuredItem.priority} Priority",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (isWhiteWidget) Color(0xFFB91C1C) else Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    // Full Description
                                    if (featuredItem.description.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = featuredItem.description.replace("#", "").replace("*", "").trim(),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = if (isWhiteWidget) Color(0xFF334155) else Color.White.copy(alpha = 0.85f),
                                                fontSize = 12.sp,
                                                lineHeight = 16.sp
                                            ),
                                            maxLines = 4
                                        )
                                    }

                                    // Dates footer
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        if (featuredItem.dueDate != null && featuredItem.dueDate > 0) {
                                            Text(
                                                text = "📅 Due " + SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(featuredItem.dueDate)),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 10.sp,
                                                    color = if (isWhiteWidget) Color(0xFFDB2777) else Color(0xFFF472B6),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.width(1.dp))
                                        }

                                        Text(
                                            text = "Created " + SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(featuredItem.createdAt)),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                color = if (isWhiteWidget) Color(0xFF64748B) else Color.White.copy(alpha = 0.6f)
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // 3. SIMULATOR FOR "ALL ITEMS" & "SPECIFIC LIST"
                        else {
                            // Header with Dropdown Menu on List Name (for Specific List)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (widgetConfig.widgetType == WidgetType.SPECIFIC_LIST) {
                                    // Interactive Dropdown Trigger on List Name in Widget!
                                    Box {
                                        Surface(
                                            onClick = { listDropdownOpen = true },
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isWhiteWidget) Color(0xFFF1F5F9) else Color.White.copy(alpha = 0.2f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isWhiteWidget) Color(0xFFCBD5E1) else Color.White.copy(alpha = 0.25f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = activeList?.name ?: "All Items",
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isWhiteWidget) Color(0xFF0F172A) else Color.White
                                                    )
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "▾",
                                                    color = if (isWhiteWidget) Color(0xFF4F46E5) else Color.White,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = listDropdownOpen,
                                            onDismissRequest = { listDropdownOpen = false }
                                        ) {
                                            lists.forEach { l ->
                                                DropdownMenuItem(
                                                    text = { Text(l.name) },
                                                    onClick = {
                                                        onUpdateWidgetConfig(widgetConfig.copy(listId = l.id))
                                                        listDropdownOpen = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // All Items Header
                                    Text(
                                        text = if (widgetConfig.categoryFilter != "ALL") "All Items • ${widgetConfig.categoryFilter}" else "All Items",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isWhiteWidget) Color(0xFF0F172A) else Color.White
                                        )
                                    )
                                }

                                Surface(
                                    onClick = { onQuickAddClick(activeList?.id ?: 0L) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isWhiteWidget) Color(0xFF4F46E5) else Color.White.copy(alpha = 0.25f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isWhiteWidget) Color(0xFF4338CA) else Color.White.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Text(
                                        text = "+ Quick Add",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Widget Items
                            if (displayedItems.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No items match widget filter.\nTap + Quick Add to create one.",
                                        color = if (isWhiteWidget) Color(0xFF64748B) else Color.White.copy(alpha = 0.7f),
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            } else {
                                displayedItems.take(5).forEach { item ->
                                    val isTask = item.type == "TASK"
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onClickItem(item) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isWhiteWidget) Color(0xFFF1F5F9) else Color.White.copy(alpha = 0.15f)
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isWhiteWidget) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.1f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Interactive checkbox or bullet
                                            if (isTask) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(
                                                            if (item.isChecked) Color(0xFF10B981)
                                                            else (if (isWhiteWidget) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.2f))
                                                        )
                                                        .border(
                                                            1.dp,
                                                            if (item.isChecked) Color(0xFF10B981)
                                                            else (if (isWhiteWidget) Color(0xFFCBD5E1) else Color.White.copy(alpha = 0.5f)),
                                                            RoundedCornerShape(6.dp)
                                                        )
                                                        .clickable { onToggleTaskChecked(item.id, !item.isChecked) },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (item.isChecked) {
                                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isWhiteWidget) Color(0xFF64748B) else Color.White)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.title,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (isWhiteWidget) {
                                                            if (item.isChecked) Color(0xFF94A3B8) else Color(0xFF0F172A)
                                                        } else {
                                                            if (item.isChecked) Color.White.copy(alpha = 0.5f) else Color.White
                                                        },
                                                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                                                    ),
                                                    maxLines = 1
                                                )

                                                if (widgetConfig.showDescription && item.description.isNotBlank()) {
                                                    Text(
                                                        text = item.description.replace("#", "").replace("*", "").trim(),
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontSize = 11.sp,
                                                            color = if (isWhiteWidget) Color(0xFF475569) else Color.White.copy(alpha = 0.75f)
                                                        ),
                                                        maxLines = 1
                                                    )
                                                }

                                                if (item.dueDate != null && item.dueDate > 0) {
                                                    Text(
                                                        text = "📅 " + SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(item.dueDate)),
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 9.sp,
                                                            color = if (isWhiteWidget) Color(0xFFDB2777) else Color(0xFFF472B6),
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    )
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

        // Section: Widget Configuration Options
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Customize Widget Settings",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Type Specific Settings:
                    // 1. SPECIFIC LIST: Target list picker
                    if (widgetConfig.widgetType == WidgetType.SPECIFIC_LIST) {
                        Text("Default List for Dropdown", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        Spacer(modifier = Modifier.height(4.dp))
                        Box {
                            Surface(
                                onClick = { listDropdownOpen = true },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(activeList?.name ?: "All Items", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                    Text("Change ⌄", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // 2. ALL ITEMS: Category & Tag filters
                    if (widgetConfig.widgetType == WidgetType.ALL_ITEMS) {
                        Text("Filter by Category", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val catFilterOptions = listOf("ALL") + categories.map { it.name }
                            catFilterOptions.forEach { catName ->
                                val isSelected = widgetConfig.categoryFilter.equals(catName, ignoreCase = true)
                                Surface(
                                    onClick = { onUpdateWidgetConfig(widgetConfig.copy(categoryFilter = catName)) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ) {
                                    Text(
                                        text = if (catName == "ALL") "All Categories" else catName,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // 3. SPECIFIC ITEM: Item selector & Main Image Picker
                    if (widgetConfig.widgetType == WidgetType.SPECIFIC_ITEM) {
                        Text("Select Item to Feature in Widget", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        Spacer(modifier = Modifier.height(4.dp))
                        Box {
                            Surface(
                                onClick = { itemPickerDropdownOpen = true },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(featuredItem?.title ?: "Select an item...", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Pick ⌄", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            DropdownMenu(
                                expanded = itemPickerDropdownOpen,
                                onDismissRequest = { itemPickerDropdownOpen = false }
                            ) {
                                allItems.forEach { item ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(item.title, fontWeight = FontWeight.SemiBold)
                                                Text("${item.category} • ${item.priority} Priority", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        },
                                        onClick = {
                                            onUpdateWidgetConfig(widgetConfig.copy(itemId = item.id))
                                            itemPickerDropdownOpen = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Main Image Section
                        Text("Widget Main Image", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pick Main Image", fontSize = 12.sp)
                            }

                            if (!widgetConfig.customImageUri.isNullOrBlank()) {
                                OutlinedButton(
                                    onClick = { onUpdateWidgetConfig(widgetConfig.copy(customImageUri = null)) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Remove Image", fontSize = 12.sp)
                                }
                            }
                        }
                        Text(
                            text = "If no image is selected, the widget goes straight to showing description and details.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Widget Theme Style (12 Options)
                    Text("Widget Gradient & Color Style (Including White)", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        widgetThemeOptions.forEach { opt ->
                            val isSelected = widgetConfig.themeStyle.equals(opt.key, ignoreCase = true)
                            Surface(
                                onClick = { onUpdateWidgetConfig(widgetConfig.copy(themeStyle = opt.key)) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(opt.previewColor)
                                            .border(1.dp, if (opt.isLight) Color(0xFFCBD5E1) else Color.Transparent, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = opt.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Text Size Option
                    Text(
                        text = "WIDGET TEXT SIZE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val sizes = listOf("SMALL" to "Small", "MEDIUM" to "Medium", "LARGE" to "Large", "XLARGE" to "X-Large")
                        sizes.forEach { (key, label) ->
                            val isSelected = widgetConfig.textSizeScale.equals(key, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onUpdateWidgetConfig(widgetConfig.copy(textSizeScale = key)) },
                                label = { Text(label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Show description in widget toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Show Note/Item Descriptions", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                            Text("Display rich snippets below item title in widget", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                        Switch(
                            checked = widgetConfig.showDescription,
                            onCheckedChange = { onUpdateWidgetConfig(widgetConfig.copy(showDescription = it)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Hide checked items in widget toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Hide Checked Tasks in Widget", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                            Text("Only show pending tasks on your homescreen", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                        Switch(
                            checked = widgetConfig.hideChecked,
                            onCheckedChange = { onUpdateWidgetConfig(widgetConfig.copy(hideChecked = it)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Pin Widget to Homescreen Button
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
                                val provider = ComponentName(context, ListAppWidgetProvider::class.java)
                                if (appWidgetManager != null && appWidgetManager.isRequestPinAppWidgetSupported) {
                                    val pinnedSuccessCallback = PendingIntent.getBroadcast(
                                        context, 0,
                                        Intent(context, ListAppWidgetProvider::class.java),
                                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                    )
                                    appWidgetManager.requestPinAppWidget(provider, null, pinnedSuccessCallback)
                                    Toast.makeText(context, "Adding widget to home screen...", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Long-press your homescreen to add the ListWidget", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, "Long-press your homescreen to add the ListWidget", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Widgets, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Widget to Home Screen")
                    }
                }
            }
        }

        // Section: App Customization & Settings
        item {
            Text(
                text = "APP THEME & CUSTOMIZATION",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        // Theme Mode (Light / Dark / System)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Theme Appearance", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppThemeMode.entries.forEach { mode ->
                            val isSelected = themeMode == mode
                            Surface(
                                onClick = { onSetThemeMode(mode) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Color Palette Presets
                    Text("Color Palette Preset", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ColorPalettePreset.entries.forEach { preset ->
                            val isSelected = colorPalette == preset
                            val pColor = Color(android.graphics.Color.parseColor(preset.primaryHex))
                            Surface(
                                onClick = { onSetColorPalette(preset) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) pColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) pColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(pColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = preset.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) pColor else MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Font Family Options
                    Text("App Typography Style", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppFontFamily.entries.forEach { font ->
                            val isSelected = fontFamily == font
                            Surface(
                                onClick = { onSetFontFamily(font) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = font.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Show Total Task/Note Counts in-app Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Show Task & Note Counters", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                            Text("Display completion statistics & counts in lists", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                        Switch(
                            checked = showCounts,
                            onCheckedChange = onSetShowCounts
                        )
                    }
                }
            }
        }

        // Section: About & App Updates
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_updates_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("About & Updates", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text("Check GitHub releases for the latest app updates & APKs", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (!isCheckingUpdates) {
                                    isCheckingUpdates = true
                                    scope.launch {
                                        try {
                                            val result = fetchLatestGitHubRelease(context)
                                            if (result.isUpdateAvailable) {
                                                updateResultDialogData = result
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "ListWidget is up to date (v${result.currentVersion})",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        } catch (_: Throwable) {
                                            Toast.makeText(
                                                context,
                                                "Unable to check for updates. Please check network connection.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } finally {
                                            isCheckingUpdates = false
                                        }
                                    }
                                }
                            },
                            enabled = !isCheckingUpdates,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("check_updates_button")
                        ) {
                            if (isCheckingUpdates) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Checking...", fontSize = 12.sp, maxLines = 1)
                            } else {
                                Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Check Updates", fontSize = 12.sp, maxLines = 1)
                            }
                        }

                        OutlinedButton(
                            onClick = { showFeedbackDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("send_feedback_button")
                        ) {
                            Icon(Icons.Default.Feedback, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Feedback", fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }
        }

        // Section: Data Backup & Restore
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_backup_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Data Backup & Restore", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text("Export all lists, items, tags & settings to a JSON file, or restore from a backup", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val timeStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                createJsonLauncher.launch("ListWidget_Backup_$timeStr.json")
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_backup_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export (JSON)", fontSize = 12.sp, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = {
                                openJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_backup_button")
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore (JSON)", fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }
        }

        // Section: Custom Categories Manager
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_categories_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Category Management",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Customize categories, icons & colors (${categories.size})",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            newCategoryNameInput = ""
                            newCategoryIconInput = "tag"
                            newCategoryColorInput = "#0284C7"
                            showAddCategoryDialog = true
                        },
                        shape = RoundedCornerShape(25.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Category", fontSize = 12.sp, maxLines = 1)
                    }
                }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Categories List
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        categories.forEach { cat ->
                            val catIcon = getVectorForCategory(cat.name, cat.iconKey)
                            val catColor = try {
                                Color(android.graphics.Color.parseColor(cat.colorHex))
                            } catch (_: Exception) {
                                MaterialTheme.colorScheme.primary
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, catColor.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Category Icon Badge
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(catColor.copy(alpha = 0.18f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = catIcon,
                                            contentDescription = cat.name,
                                            tint = catColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Category Name & Type info
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = cat.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            if (cat.isDefault) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                ) {
                                                    Text(
                                                        text = "Default",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 10.sp,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            fontWeight = FontWeight.SemiBold
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "Icon: ${cat.iconKey}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }

                                    // Color Dot Indicator
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(catColor)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Edit Action Button
                                    IconButton(
                                        onClick = {
                                            categoryToEdit = cat
                                            editCategoryNameInput = cat.name
                                            editCategoryIconInput = cat.iconKey
                                            editCategoryColorInput = cat.colorHex
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit ${cat.name}",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    // Delete Action Button (Enabled for non-default or custom)
                                    if (!cat.isDefault) {
                                        IconButton(
                                            onClick = { categoryToDelete = cat },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete ${cat.name}",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Reset button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { onResetCategories() }
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset Default Categories", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Section: Priorities Manager (Default and Custom)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Priority Tags", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text(
                                "Customize labels and colors for defaults (None, Low, Medium, High) and custom tags",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        priorities.forEach { p ->
                            val isDefault = p.id == "NONE" || p.id == "LOW" || p.id == "MEDIUM" || p.id == "HIGH"
                            val color = try { Color(android.graphics.Color.parseColor(p.colorHex)) } catch (_: Exception) { Color.Gray }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = color.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f)),
                                modifier = Modifier.clickable {
                                    priorityToEdit = p
                                    editPriorityLabelInput = p.label
                                    editPriorityColorInput = p.colorHex
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = p.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = color,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )

                                    if (isDefault) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = color.copy(alpha = 0.2f),
                                            modifier = Modifier.padding(horizontal = 2.dp)
                                        ) {
                                            Text(
                                                text = "Default",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 8.sp,
                                                    color = color,
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }

                                    // Edit button
                                    IconButton(
                                        onClick = {
                                            priorityToEdit = p
                                            editPriorityLabelInput = p.label
                                            editPriorityColorInput = p.colorHex
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Priority Tag",
                                            tint = color,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    // Delete button (for custom priorities)
                                    if (!isDefault) {
                                        IconButton(
                                            onClick = { priorityToDelete = p },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Priority Tag",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Add Custom Priority Tag", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant))
                    Spacer(modifier = Modifier.height(6.dp))

                    // Color picker for new priority
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        priorityColorPresets.take(8).forEach { hex ->
                            val col = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Gray }
                            val isSelected = newPriorityColor.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { newPriorityColor = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Add custom priority input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newPriorityLabel,
                            onValueChange = { newPriorityLabel = it },
                            placeholder = { Text("New priority name...", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                if (newPriorityLabel.isNotBlank()) {
                                    onAddPriority(newPriorityLabel.trim(), newPriorityColor)
                                    newPriorityLabel = ""
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Add")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Reset Priorities to Defaults
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { onResetPriorities() }
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset Default Priorities", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // ADD NEW WIDGET DIALOG
    if (showAddWidgetDialog) {
        AlertDialog(
            onDismissRequest = { showAddWidgetDialog = false },
            title = { Text("Create New Widget", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newWidgetNameInput,
                        onValueChange = { newWidgetNameInput = it },
                        label = { Text("Widget Name") },
                        placeholder = { Text("e.g. Work Tasks, Shopping List...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Select Initial Widget Type:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        WidgetType.entries.forEach { type ->
                            val isSelected = newWidgetTypeSelected == type
                            Surface(
                                onClick = { newWidgetTypeSelected = type },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = type.label,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddNewWidget(newWidgetNameInput.ifBlank { null }, newWidgetTypeSelected)
                        showAddWidgetDialog = false
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Create Widget")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showAddWidgetDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // RENAME WIDGET DIALOG
    if (showRenameWidgetDialog) {
        AlertDialog(
            onDismissRequest = { showRenameWidgetDialog = false },
            title = { Text("Rename Widget", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameWidgetInput,
                    onValueChange = { renameWidgetInput = it },
                    label = { Text("Widget Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameWidgetInput.isNotBlank()) {
                            onUpdateWidgetConfig(widgetConfig.copy(widgetName = renameWidgetInput.trim()))
                            showRenameWidgetDialog = false
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showRenameWidgetDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // ADD CATEGORY DIALOG
    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = {
                Text(
                    text = "Add New Category",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newCategoryNameInput,
                        onValueChange = { newCategoryNameInput = it },
                        label = { Text("Category Name") },
                        placeholder = { Text("e.g. Work, Gym, Books...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Category Icon",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    IconPickerGrid(
                        selectedIconName = newCategoryIconInput,
                        onSelectIcon = { newCategoryIconInput = it },
                        accentColor = try { Color(android.graphics.Color.parseColor(newCategoryColorInput)) } catch (_: Exception) { MaterialTheme.colorScheme.primary }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Category Color",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoryColorPresets.forEach { colorHex ->
                            val c = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (_: Exception) { Color.Gray }
                            val isColorSelected = newCategoryColorInput.equals(colorHex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(
                                        width = if (isColorSelected) 3.dp else 1.dp,
                                        color = if (isColorSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { newCategoryColorInput = colorHex },
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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategoryNameInput.isNotBlank()) {
                            onAddCategory(newCategoryNameInput.trim(), newCategoryIconInput, newCategoryColorInput)
                            showAddCategoryDialog = false
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showAddCategoryDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // EDIT CATEGORY DIALOG
    categoryToEdit?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryToEdit = null },
            title = {
                Text(
                    text = "Edit Category",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = editCategoryNameInput,
                        onValueChange = { editCategoryNameInput = it },
                        label = { Text("Category Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Category Icon",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    IconPickerGrid(
                        selectedIconName = editCategoryIconInput,
                        onSelectIcon = { editCategoryIconInput = it },
                        accentColor = try { Color(android.graphics.Color.parseColor(editCategoryColorInput)) } catch (_: Exception) { MaterialTheme.colorScheme.primary }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Category Color",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoryColorPresets.forEach { colorHex ->
                            val c = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (_: Exception) { Color.Gray }
                            val isColorSelected = editCategoryColorInput.equals(colorHex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(
                                        width = if (isColorSelected) 3.dp else 1.dp,
                                        color = if (isColorSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { editCategoryColorInput = colorHex },
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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editCategoryNameInput.isNotBlank()) {
                            onUpdateCategory(
                                cat.id,
                                editCategoryNameInput.trim(),
                                editCategoryIconInput,
                                editCategoryColorInput
                            )
                            categoryToEdit = null
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { categoryToEdit = null },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // EDIT PRIORITY DIALOG (Supports default and custom tags)
    priorityToEdit?.let { pri ->
        val isDefaultPri = pri.id == "NONE" || pri.id == "LOW" || pri.id == "MEDIUM" || pri.id == "HIGH"
        AlertDialog(
            onDismissRequest = { priorityToEdit = null },
            title = {
                Text(
                    text = if (isDefaultPri) "Edit Default Priority (${pri.id})" else "Edit Priority Tag",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = editPriorityLabelInput,
                        onValueChange = { editPriorityLabelInput = it },
                        label = { Text("Priority Tag Label") },
                        placeholder = { Text("e.g. Urgent, High, Today...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Choose Tag Accent Color",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        priorityColorPresets.forEach { colorHex ->
                            val color = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (_: Exception) { Color.Gray }
                            val isColorSelected = editPriorityColorInput.equals(colorHex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isColorSelected) 3.dp else 1.dp,
                                        color = if (isColorSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { editPriorityColorInput = colorHex },
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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editPriorityLabelInput.isNotBlank()) {
                            onUpdatePriority(
                                pri.id,
                                editPriorityLabelInput.trim(),
                                editPriorityColorInput
                            )
                            priorityToEdit = null
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { priorityToEdit = null },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // UPDATE RESULT DIALOG
    if (updateResultDialogData != null) {
        val info = updateResultDialogData!!
        AlertDialog(
            onDismissRequest = { updateResultDialogData = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Version Available!", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Version ${info.latestTag} is available on GitHub! (Installed: v${info.currentVersion})",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )

                    if (info.releaseNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Release Notes:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 160.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(10.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = info.releaseNotes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!info.apkUrl.isNullOrBlank()) {
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.apkUrl))
                                    context.startActivity(intent)
                                } catch (_: Throwable) {
                                    Toast.makeText(context, "Unable to open download link", Toast.LENGTH_SHORT).show()
                                }
                                updateResultDialogData = null
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download APK")
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.releaseUrl))
                                context.startActivity(intent)
                            } catch (_: Throwable) {}
                            updateResultDialogData = null
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("View on GitHub")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { updateResultDialogData = null }) {
                    Text("Close")
                }
            }
        )
    }

    // DELETE PRIORITY CONFIRMATION DIALOG
    priorityToDelete?.let { pri ->
        AlertDialog(
            onDismissRequest = { priorityToDelete = null },
            title = { Text("Delete Priority Tag") },
            text = { Text("Are you sure you want to delete the \"${pri.label}\" priority tag?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePriority(pri.id)
                        priorityToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { priorityToDelete = null },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // DELETE CATEGORY CONFIRMATION DIALOG
    categoryToDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Delete Category") },
            text = { Text("Are you sure you want to delete the \"${cat.name}\" category?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCategory(cat.id)
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { categoryToDelete = null },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // FEEDBACK DIALOG
    if (showFeedbackDialog) {
        AlertDialog(
            onDismissRequest = { showFeedbackDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Feedback,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send App Feedback", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Share your feature requests, suggestions, or bug reports to help improve ListWidget.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = feedbackInput,
                        onValueChange = { feedbackInput = it },
                        placeholder = { Text("Type your feedback here...", fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 110.dp)
                            .testTag("feedback_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        sendFeedbackEmail(context, feedbackInput.trim())
                        showFeedbackDialog = false
                        feedbackInput = ""
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Send Feedback")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showFeedbackDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // RESTORE CONFIRMATION DIALOG
    if (pendingRestoreJson != null) {
        AlertDialog(
            onDismissRequest = { pendingRestoreJson = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RestorePage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restore Backup Data?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "Restoring from this file will import lists, tasks, notes, custom categories, priority tags, and settings into your app. Do you want to proceed?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val success = onRestoreBackup(pendingRestoreJson!!)
                            if (success) {
                                Toast.makeText(context, "Backup restored successfully!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Failed to restore backup data.", Toast.LENGTH_SHORT).show()
                            }
                            pendingRestoreJson = null
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Restore Backup")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { pendingRestoreJson = null },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
