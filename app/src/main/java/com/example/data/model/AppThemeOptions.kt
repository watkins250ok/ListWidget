package com.example.data.model

enum class AppThemeMode(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Light Mode"),
    DARK("Dark Mode")
}

enum class WidgetType(val label: String, val description: String) {
    ALL_LISTS("All Lists", "Displays name, icon, description & creation date of all lists"),
    ALL_ITEMS("All Items", "Displays all items with category and tag filters"),
    SPECIFIC_LIST("Specific List", "Displays items from a selected list with a dropdown switcher"),
    SPECIFIC_ITEM("Specific Item", "Features a single item with optional main image and full details")
}

enum class ColorPalettePreset(val label: String, val primaryHex: String, val secondaryHex: String, val bgHex: String) {
    PLAIN_WHITE("Plain White", "#1E293B", "#64748B", "#FFFFFF"),
    BENTO_PURPLE("Bento Lavender", "#6750A4", "#E8DEF8", "#F3EDF7"),
    PURPLE_VIOLET("Purple Glow", "#7C3AED", "#A855F7", "#1E1035"),
    MIDNIGHT_DARK("Midnight Dark", "#6366F1", "#818CF8", "#0F172A"),
    OCEAN_BLUE("Ocean Blue", "#0284C7", "#38BDF8", "#082F49"),
    EMERALD_MINT("Emerald Green", "#059669", "#34D399", "#064E3B"),
    SUNSET_AMBER("Sunset Orange", "#D97706", "#FBBF24", "#451A03"),
    ROSE_QUARTZ("Rose Pink", "#DB2777", "#F472B6", "#500724"),
    SLATE_GRAPHITE("Slate Graphite", "#475569", "#94A3B8", "#1E293B"),
    MINT_TEAL("Mint Teal", "#0D9488", "#2DD4BF", "#042F2E")
}

enum class AppFontFamily(val label: String) {
    CLEAN_SANS("Clean Sans"),
    SERIF("Classic Serif"),
    MONOSPACE("Monospace")
}

data class WidgetStyleConfig(
    val widgetId: String = "widget_1",
    val widgetName: String = "Widget 1",
    val widgetType: WidgetType = WidgetType.SPECIFIC_LIST,
    val listId: Long = 0,
    val itemId: Long = 0,
    val categoryFilter: String = "ALL", // "ALL", "Task", "Idea", "Note", or custom
    val tagFilter: String = "ALL", // "ALL" or specific tag
    val customImageUri: String? = null, // for Specific Item main image override / selection
    val colorPreset: ColorPalettePreset = ColorPalettePreset.PURPLE_VIOLET,
    val themeStyle: String = "PURPLE", // "WHITE", "PURPLE", "MIDNIGHT", "OCEAN", "EMERALD", "SUNSET", "ROSE", "SLATE", "MINT", "AMBER", "LAVENDER", "CORAL"
    val showDescription: Boolean = true,
    val hideChecked: Boolean = false,
    val textSizeScale: String = "MEDIUM" // "SMALL", "MEDIUM", "LARGE", "XLARGE"
)
