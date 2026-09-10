package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Task
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class PresetIcon(val key: String, val label: String, val icon: ImageVector)

val PRESET_LIST_ICONS = listOf(
    PresetIcon("checklist", "Checklist", Icons.Default.Checklist),
    PresetIcon("task_alt", "Tasks", Icons.Default.Task),
    PresetIcon("done", "Completed", Icons.Default.CheckCircle),
    PresetIcon("numbers", "Numbered List", Icons.Default.FormatListNumbered),
    PresetIcon("idea", "Idea", Icons.Default.Lightbulb),
    PresetIcon("note", "Note", Icons.Default.Description),
    PresetIcon("tag", "Tag", Icons.Default.Tag),
    PresetIcon("work", "Work", Icons.Default.Work),
    PresetIcon("shopping", "Shopping", Icons.Default.ShoppingCart),
    PresetIcon("grocery", "Groceries", Icons.Default.LocalGroceryStore),
    PresetIcon("mall", "Mall", Icons.Default.LocalMall),
    PresetIcon("card", "Payments", Icons.Default.Payment),
    PresetIcon("money", "Savings", Icons.Default.AttachMoney),
    PresetIcon("receipt", "Receipts", Icons.Default.Receipt),
    PresetIcon("bank", "Finance", Icons.Default.AccountBalance),
    PresetIcon("star", "Star", Icons.Default.Star),
    PresetIcon("bookmark", "Bookmark", Icons.Default.Bookmark),
    PresetIcon("heart", "Favorite", Icons.Default.Favorite),
    PresetIcon("folder", "Folder", Icons.Default.Folder),
    PresetIcon("sparkles", "Inspiration", Icons.Default.AutoAwesome),
    PresetIcon("code", "Code", Icons.Default.Code),
    PresetIcon("terminal", "Terminal", Icons.Default.Terminal),
    PresetIcon("fitness", "Fitness", Icons.Default.FitnessCenter),
    PresetIcon("sports", "Sports", Icons.Default.SportsBasketball),
    PresetIcon("health", "Health", Icons.Default.LocalHospital),
    PresetIcon("book", "Reading", Icons.Default.MenuBook),
    PresetIcon("school", "Study", Icons.Default.School),
    PresetIcon("home", "Home", Icons.Default.Home),
    PresetIcon("person", "Personal", Icons.Default.Person),
    PresetIcon("team", "Group", Icons.Default.Group),
    PresetIcon("flag", "Important", Icons.Default.Flag),
    PresetIcon("notifications", "Reminders", Icons.Default.Notifications),
    PresetIcon("calendar", "Calendar", Icons.Default.Event),
    PresetIcon("time", "Schedule", Icons.Default.Schedule),
    PresetIcon("timer", "Timer", Icons.Default.Timer),
    PresetIcon("music", "Music", Icons.Default.MusicNote),
    PresetIcon("audio", "Audio", Icons.Default.Headset),
    PresetIcon("palette", "Design", Icons.Default.Palette),
    PresetIcon("camera", "Photos", Icons.Default.CameraAlt),
    PresetIcon("movie", "Cinema", Icons.Default.Movie),
    PresetIcon("tv", "TV", Icons.Default.Tv),
    PresetIcon("gaming", "Gaming", Icons.Default.Games),
    PresetIcon("pin", "Pinned", Icons.Default.PushPin),
    PresetIcon("location", "Location", Icons.Default.LocationOn),
    PresetIcon("explore", "Explore", Icons.Default.Explore),
    PresetIcon("world", "Global", Icons.Default.Language),
    PresetIcon("flight", "Flight", Icons.Default.Flight),
    PresetIcon("hotel", "Hotel", Icons.Default.Hotel),
    PresetIcon("cafe", "Coffee", Icons.Default.LocalCafe),
    PresetIcon("dining", "Dining", Icons.Default.Restaurant),
    PresetIcon("food", "Fast Food", Icons.Default.Fastfood),
    PresetIcon("car", "Travel", Icons.Default.DirectionsCar),
    PresetIcon("party", "Events", Icons.Default.Celebration),
    PresetIcon("pets", "Pets", Icons.Default.Pets),
    PresetIcon("mail", "Email", Icons.Default.Mail),
    PresetIcon("phone", "Phone", Icons.Default.Call),
    PresetIcon("tools", "Maintenance", Icons.Default.Build),
    PresetIcon("lock", "Privacy", Icons.Default.Lock),
    PresetIcon("key", "Keys", Icons.Default.Key),
    PresetIcon("security", "Security", Icons.Default.Security),
    PresetIcon("cloud", "Cloud", Icons.Default.Cloud),
    PresetIcon("analytics", "Analytics", Icons.Default.Analytics),
    PresetIcon("trending", "Growth", Icons.AutoMirrored.Filled.TrendingUp),
    PresetIcon("verified", "Verified", Icons.Default.Verified)
)

fun getVectorForIconName(iconName: String): ImageVector {
    return PRESET_LIST_ICONS.find { it.key.equals(iconName, ignoreCase = true) }?.icon ?: when (iconName.lowercase()) {
        "idea", "lightbulb" -> Icons.Default.Lightbulb
        "note", "description" -> Icons.Default.Description
        "checklist", "task" -> Icons.Default.Checklist
        else -> Icons.Default.Tag
    }
}

fun getVectorForCategory(categoryName: String, iconKey: String? = null): ImageVector {
    if (!iconKey.isNullOrBlank()) {
        val found = PRESET_LIST_ICONS.find { it.key.equals(iconKey, ignoreCase = true) }
        if (found != null) return found.icon
    }
    return when (categoryName.trim().lowercase()) {
        "idea", "ideas" -> Icons.Default.Lightbulb
        "task", "tasks" -> Icons.Default.Checklist
        "note", "notes" -> Icons.Default.Description
        "work" -> Icons.Default.Work
        "shopping", "shop", "grocery", "groceries" -> Icons.Default.ShoppingCart
        "fitness", "gym", "workout", "sports" -> Icons.Default.FitnessCenter
        "health", "medical" -> Icons.Default.LocalHospital
        "reading", "book", "books" -> Icons.Default.MenuBook
        "code", "coding", "dev", "terminal" -> Icons.Default.Code
        "personal" -> Icons.Default.Person
        "home" -> Icons.Default.Home
        "study", "school" -> Icons.Default.School
        "design", "art", "palette" -> Icons.Default.Palette
        "finance", "money", "bank", "savings" -> Icons.Default.AttachMoney
        "food", "dining", "restaurant" -> Icons.Default.Restaurant
        "travel", "flight" -> Icons.Default.Flight
        "events", "party", "celebration" -> Icons.Default.Celebration
        else -> Icons.Default.Tag
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconPickerGrid(
    selectedIconName: String,
    onSelectIcon: (String) -> Unit,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PRESET_LIST_ICONS.forEach { preset ->
            val isSelected = preset.key.equals(selectedIconName, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) accentColor.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelectIcon(preset.key) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = preset.icon,
                    contentDescription = preset.label,
                    tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
