package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.io.IOException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestorePage
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
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
    showCounts: Boolean = true,
    priorities: List<PriorityOption> = PriorityOption.DEFAULT_PRIORITIES,
    categories: List<CategoryOption> = CategoryOption.DEFAULT_CATEGORIES,
    itemTags: List<String> = listOf("#idea", "#home", "#work"),
    onUpdateWidgetConfig: (WidgetStyleConfig) -> Unit = {},
    onSetThemeMode: (AppThemeMode) -> Unit = {},
    onSetColorPalette: (ColorPalettePreset) -> Unit = {},
    onSetFontFamily: (AppFontFamily) -> Unit = {},
    onSetShowCounts: (Boolean) -> Unit = {},
    onExportBackup: suspend () -> String = { "" },
    onRestoreBackup: suspend (String) -> Boolean = { false },
    onAddPriority: (String, String) -> Unit = { _, _ -> },
    onUpdatePriority: (String, String, String) -> Unit = { _, _, _ -> },
    onDeletePriority: (String) -> Unit = {},
    onResetPriorities: () -> Unit = {},
    onAddCategory: (String, String, String) -> Unit = { _, _, _ -> },
    onUpdateCategory: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    onDeleteCategory: (String) -> Unit = {},
    onResetCategories: () -> Unit = {},
    onAddTag: (String) -> Unit = {},
    onUpdateTag: (String, String) -> Unit = { _, _ -> },
    onDeleteTag: (String) -> Unit = {},
    onToggleTaskChecked: (Long, Boolean) -> Unit = { _, _ -> },
    onQuickAddClick: (Long) -> Unit = {},
    onClickItem: (ListItemEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showAboutDialog by remember { mutableStateOf(false) }
    var isCheckingUpdates by remember { mutableStateOf(false) }
    var updateResultDialogData by remember { mutableStateOf<AppUpdateResult?>(null) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var feedbackInput by remember { mutableStateOf("") }
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }

    // Color Palette Expand/Collapse State
    var colorPaletteExpanded by remember { mutableStateOf(false) }

    // Priority Tag State
    var showAddPriorityDialog by remember { mutableStateOf(false) }
    var newPriorityLabelInput by remember { mutableStateOf("") }
    var newPriorityColorInput by remember { mutableStateOf("#EF4444") }
    var priorityToEdit by remember { mutableStateOf<PriorityOption?>(null) }
    var editPriorityLabelInput by remember { mutableStateOf("") }
    var editPriorityColorInput by remember { mutableStateOf("#EF4444") }

    // Category State
    val forbiddenCategoryNames = remember { setOf("places to go", "further research", "tech", "check this out") }
    val displayCategories = remember(categories) {
        categories.filterNot { forbiddenCategoryNames.contains(it.name.trim().lowercase()) }
    }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryNameInput by remember { mutableStateOf("") }
    var newCategoryIconInput by remember { mutableStateOf("tag") }
    var newCategoryColorInput by remember { mutableStateOf("#0284C7") }
    var categoryToEdit by remember { mutableStateOf<CategoryOption?>(null) }
    var editCategoryNameInput by remember { mutableStateOf("") }
    var editCategoryIconInput by remember { mutableStateOf("tag") }
    var editCategoryColorInput by remember { mutableStateOf("#0284C7") }

    // Tag State
    var showAddTagDialog by remember { mutableStateOf(false) }
    var newTagNameInput by remember { mutableStateOf("") }
    var tagToEdit by remember { mutableStateOf<String?>(null) }
    var editTagNameInput by remember { mutableStateOf("") }

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

    val priorityColorPresets = listOf(
        "#64748B", "#3B82F6", "#0284C7", "#10B981",
        "#14B8A6", "#EAB308", "#F59E0B", "#F97316",
        "#EF4444", "#EC4899", "#8B5CF6", "#6366F1"
    )

    val categoryColorPresets = listOf(
        "#6366F1", "#F59E0B", "#EC4899", "#10B981",
        "#3B82F6", "#8B5CF6", "#0284C7", "#EF4444", "#14B8A6", "#F97316"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("widget_studio_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
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
                        text = "App appearance, categories, tags & backup",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        }

        // Section 1: App Appearance
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("App Appearance", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("Customize theme mode, color palette & app font", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))

                    Spacer(modifier = Modifier.height(14.dp))

                    // Theme Mode Picker
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

                    // Color Palette (Expandable, Collapsed by Default)
                    Surface(
                        onClick = { colorPaletteExpanded = !colorPaletteExpanded },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val currentPColor = try {
                                    Color(android.graphics.Color.parseColor(colorPalette.primaryHex))
                                } catch (_: Exception) {
                                    MaterialTheme.colorScheme.primary
                                }
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(currentPColor)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Color Palette", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                                    Text(colorPalette.label, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp))
                                }
                            }
                            Text(
                                text = if (colorPaletteExpanded) "Collapse ▴" else "Change ▾",
                                style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    if (colorPaletteExpanded) {
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ColorPalettePreset.entries.forEach { preset ->
                                val isSelected = colorPalette == preset
                                val pColor = try {
                                    Color(android.graphics.Color.parseColor(preset.primaryHex))
                                } catch (_: Exception) {
                                    MaterialTheme.colorScheme.primary
                                }
                                Surface(
                                    onClick = { onSetColorPalette(preset) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) pColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = BorderStroke(
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
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // App Typography Style (Three Options: Clean Sans, Classic Serif, Monospace)
                    Text("App Typography Style", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppFontFamily.entries.forEach { font ->
                            val isSelected = fontFamily == font
                            Surface(
                                onClick = { onSetFontFamily(font) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = font.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Item Categories
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Item Categories", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        IconButton(
                            onClick = {
                                newCategoryNameInput = ""
                                newCategoryIconInput = "tag"
                                newCategoryColorInput = "#0284C7"
                                showAddCategoryDialog = true
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Category", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        displayCategories.forEach { cat ->
                            val catColor = try {
                                Color(android.graphics.Color.parseColor(cat.colorHex))
                            } catch (_: Exception) {
                                MaterialTheme.colorScheme.primary
                            }
                            val catIcon = getVectorForCategory(cat.name, cat.iconKey)

                            Surface(
                                onClick = {
                                    categoryToEdit = cat
                                    editCategoryNameInput = cat.name
                                    editCategoryIconInput = cat.iconKey
                                    editCategoryColorInput = cat.colorHex
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = catColor.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, catColor.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = catIcon,
                                        contentDescription = null,
                                        tint = catColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = cat.name,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Priority Tags
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Priority Tags", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        IconButton(
                            onClick = {
                                newPriorityLabelInput = ""
                                newPriorityColorInput = "#EF4444"
                                showAddPriorityDialog = true
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Priority", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        priorities.forEach { pri ->
                            val priColor = try {
                                Color(android.graphics.Color.parseColor(pri.colorHex))
                            } catch (_: Exception) {
                                MaterialTheme.colorScheme.primary
                            }

                            val displayLabel = when (pri.label.trim().uppercase()) {
                                "LP/LE" -> "None"
                                "LP/HE" -> "Low"
                                "HP/LE" -> "Medium"
                                "HP/HE" -> "High"
                                else -> pri.label
                            }

                            Surface(
                                onClick = {
                                    priorityToEdit = pri
                                    editPriorityLabelInput = displayLabel
                                    editPriorityColorInput = pri.colorHex
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = priColor.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, priColor.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(priColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = displayLabel,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 4: Item Tags
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Item Tags", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        IconButton(
                            onClick = {
                                newTagNameInput = ""
                                showAddTagDialog = true
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Tag", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemTags.forEach { tagStr ->
                            val cleanTag = tagStr.replace("#", "").trim()
                            val formattedLabel = "#$cleanTag"

                            Surface(
                                onClick = {
                                    tagToEdit = tagStr
                                    editTagNameInput = cleanTag
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formattedLabel,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 5: About & Updates
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_updates_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("About & Updates", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("App info, updates, feedback & support", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))

                    Spacer(modifier = Modifier.height(14.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showAboutDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("About", fontSize = 12.sp, maxLines = 2)
                        }

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
                                Text("Checking...", fontSize = 12.sp, maxLines = 2)
                            } else {
                                Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Update", fontSize = 12.sp, maxLines = 2)
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
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Feedback", fontSize = 12.sp, maxLines = 2)
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ko-fi.com/amywatkins"))
                                    context.startActivity(intent)
                                } catch (_: Throwable) {
                                    Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Support", fontSize = 12.sp, maxLines = 2)
                        }
                    }
                }
            }
        }

        // Section 6: Data Backup & Restore
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_backup_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Data Backup & Restore", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("Export all lists, items, tags & settings to a JSON file, or restore from a backup", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))

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
    }

    // --- DIALOGS ---

    // ABOUT DIALOG
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About ListWidget", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("ListWidget v1.0", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("A feature-rich list & task management app with interactive home screen widgets.", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // ADD CATEGORY DIALOG
    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Add New Category", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newCategoryNameInput,
                        onValueChange = { newCategoryNameInput = it },
                        label = { Text("Category Name") },
                        placeholder = { Text("e.g. Work, Gym, Books, Purchase...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Category Icon", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(6.dp))
                    IconPickerGrid(
                        selectedIconName = newCategoryIconInput,
                        onSelectIcon = { newCategoryIconInput = it },
                        accentColor = try { Color(android.graphics.Color.parseColor(newCategoryColorInput)) } catch (_: Exception) { MaterialTheme.colorScheme.primary }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Category Color", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
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
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
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
                OutlinedButton(onClick = { showAddCategoryDialog = false }, shape = RoundedCornerShape(10.dp)) {
                    Text("Cancel")
                }
            }
        )
    }

    // EDIT CATEGORY DIALOG
    categoryToEdit?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryToEdit = null },
            title = { Text("Edit Category", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = editCategoryNameInput,
                        onValueChange = { editCategoryNameInput = it },
                        label = { Text("Category Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Category Icon", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(6.dp))
                    IconPickerGrid(
                        selectedIconName = editCategoryIconInput,
                        onSelectIcon = { editCategoryIconInput = it },
                        accentColor = try { Color(android.graphics.Color.parseColor(editCategoryColorInput)) } catch (_: Exception) { MaterialTheme.colorScheme.primary }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Category Color", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
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
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            onDeleteCategory(cat.id)
                            categoryToEdit = null
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Delete")
                    }
                    Button(
                        onClick = {
                            if (editCategoryNameInput.isNotBlank()) {
                                onUpdateCategory(cat.id, editCategoryNameInput.trim(), editCategoryIconInput, editCategoryColorInput)
                                categoryToEdit = null
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { categoryToEdit = null }, shape = RoundedCornerShape(10.dp)) {
                    Text("Cancel")
                }
            }
        )
    }

    // ADD PRIORITY TAG DIALOG
    if (showAddPriorityDialog) {
        AlertDialog(
            onDismissRequest = { showAddPriorityDialog = false },
            title = { Text("Add Priority Tag", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newPriorityLabelInput,
                        onValueChange = { newPriorityLabelInput = it },
                        label = { Text("Priority Label") },
                        placeholder = { Text("e.g. Urgent, Critical, Later...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Tag Color", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        priorityColorPresets.forEach { colorHex ->
                            val color = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (_: Exception) { Color.Gray }
                            val isColorSelected = newPriorityColorInput.equals(colorHex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isColorSelected) 3.dp else 1.dp,
                                        color = if (isColorSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { newPriorityColorInput = colorHex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isColorSelected) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPriorityLabelInput.isNotBlank()) {
                            onAddPriority(newPriorityLabelInput.trim(), newPriorityColorInput)
                            showAddPriorityDialog = false
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddPriorityDialog = false }, shape = RoundedCornerShape(10.dp)) {
                    Text("Cancel")
                }
            }
        )
    }

    // EDIT PRIORITY TAG DIALOG
    priorityToEdit?.let { pri ->
        AlertDialog(
            onDismissRequest = { priorityToEdit = null },
            title = { Text("Edit Priority Tag", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = editPriorityLabelInput,
                        onValueChange = { editPriorityLabelInput = it },
                        label = { Text("Priority Tag Label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Tag Color", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
                    Spacer(modifier = Modifier.height(6.dp))
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
                                    .size(32.dp)
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
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            onDeletePriority(pri.id)
                            priorityToEdit = null
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Delete")
                    }
                    Button(
                        onClick = {
                            if (editPriorityLabelInput.isNotBlank()) {
                                onUpdatePriority(pri.id, editPriorityLabelInput.trim(), editPriorityColorInput)
                                priorityToEdit = null
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { priorityToEdit = null }, shape = RoundedCornerShape(10.dp)) {
                    Text("Cancel")
                }
            }
        )
    }

    // ADD ITEM TAG DIALOG
    if (showAddTagDialog) {
        AlertDialog(
            onDismissRequest = { showAddTagDialog = false },
            title = { Text("Add Item Tag", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newTagNameInput,
                        onValueChange = { newTagNameInput = it },
                        label = { Text("Tag Name") },
                        placeholder = { Text("e.g. idea, home, work...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTagNameInput.isNotBlank()) {
                            onAddTag(newTagNameInput.trim())
                            showAddTagDialog = false
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddTagDialog = false }, shape = RoundedCornerShape(10.dp)) {
                    Text("Cancel")
                }
            }
        )
    }

    // EDIT / DELETE ITEM TAG DIALOG
    tagToEdit?.let { tagStr ->
        AlertDialog(
            onDismissRequest = { tagToEdit = null },
            title = { Text("Edit Item Tag", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = editTagNameInput,
                        onValueChange = { editTagNameInput = it },
                        label = { Text("Tag Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            onDeleteTag(tagStr)
                            tagToEdit = null
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Delete")
                    }
                    Button(
                        onClick = {
                            if (editTagNameInput.isNotBlank()) {
                                onUpdateTag(tagStr, editTagNameInput.trim())
                                tagToEdit = null
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { tagToEdit = null }, shape = RoundedCornerShape(10.dp)) {
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
                OutlinedButton(onClick = { updateResultDialogData = null }, shape = RoundedCornerShape(10.dp)) {
                    Text("Close")
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
                        if (feedbackInput.isNotBlank()) {
                            Toast.makeText(context, "Thanks for your feedback!", Toast.LENGTH_SHORT).show()
                            showFeedbackDialog = false
                            feedbackInput = ""
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Send Feedback")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showFeedbackDialog = false }, shape = RoundedCornerShape(10.dp)) {
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
                OutlinedButton(onClick = { pendingRestoreJson = null }, shape = RoundedCornerShape(10.dp)) {
                    Text("Cancel")
                }
            }
        )
    }
}
