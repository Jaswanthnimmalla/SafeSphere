package com.runanywhere.startup_hackathon20.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runanywhere.startup_hackathon20.data.*
import com.runanywhere.startup_hackathon20.viewmodels.SafeSphereViewModel
import com.runanywhere.startup_hackathon20.viewmodels.SafeSphereScreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * ENHANCED PRO-LEVEL PRIVACY VAULT SCREEN
 *
 * Features:
 * - Real-time statistics and analytics
 * - Advanced search and filter (by category, date, encryption status)
 * - Beautiful category visualization
 * - Quick actions and shortcuts
 * - Activity timeline
 * - Storage usage charts
 * - Smart sorting options
 * - Batch operations
 */
@Composable
fun EnhancedPrivacyVaultScreen(viewModel: SafeSphereViewModel) {
    val vaultItems by viewModel.vaultItems.collectAsState()
    val stats by viewModel.storageStats.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<PrivacyVaultItem?>(null) }

    // Real-time filtering and search
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<VaultCategory?>(null) }
    var sortBy by remember { mutableStateOf(VaultSortOption.RECENT) }
    var showEncryptedOnly by remember { mutableStateOf(false) }

    // Filter items in real-time
    val filteredItems =
        remember(vaultItems, searchQuery, selectedCategory, sortBy, showEncryptedOnly) {
            var items = vaultItems

            // Search filter
            if (searchQuery.isNotEmpty()) {
                items = items.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                            it.category.displayName.contains(searchQuery, ignoreCase = true)
            }
        }

        // Category filter
        if (selectedCategory != null) {
            items = items.filter { it.category == selectedCategory }
        }

        // Encryption filter
        if (showEncryptedOnly) {
            items = items.filter { it.isEncrypted }
        }

        // Sort
        when (sortBy) {
            VaultSortOption.RECENT -> items.sortedByDescending { it.modifiedAt }
            VaultSortOption.OLDEST -> items.sortedBy { it.modifiedAt }
            VaultSortOption.NAME_AZ -> items.sortedBy { it.title }
            VaultSortOption.NAME_ZA -> items.sortedByDescending { it.title }
            VaultSortOption.SIZE_DESC -> items.sortedByDescending { it.size }
            VaultSortOption.SIZE_ASC -> items.sortedBy { it.size }
        }
    }

    // Real-time analytics
    val analytics = remember(vaultItems) {
        VaultAnalytics(
            totalItems = vaultItems.size,
            encryptedItems = vaultItems.count { it.isEncrypted },
            totalSize = vaultItems.sumOf { it.size.toLong() },
            recentItems = vaultItems.count {
                it.modifiedAt > System.currentTimeMillis() - 24 * 60 * 60 * 1000
            },
            categoryBreakdown = vaultItems.groupBy { it.category }
                .mapValues { it.value.size },
            averageItemSize = if (vaultItems.isNotEmpty())
                vaultItems.sumOf { it.size.toLong() } / vaultItems.size
            else 0L
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header removed - using default top bar instead

            if (vaultItems.isEmpty()) {
                // Empty State
                EnhancedEmptyVaultState(
                    onAddFirstItem = { showAddDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    // Real-Time Analytics Dashboard
                    item {
                        VaultAnalyticsDashboard(analytics)
                    }

                    // Search and Filter Bar
                    item {
                        SearchAndFilterBar(
                            searchQuery = searchQuery,
                            onSearchChange = { searchQuery = it },
                            selectedCategory = selectedCategory,
                            onCategorySelect = { selectedCategory = it },
                            sortBy = sortBy,
                            onSortChange = { sortBy = it },
                            showEncryptedOnly = showEncryptedOnly,
                            onEncryptedToggle = { showEncryptedOnly = !showEncryptedOnly }
                        )
                    }

                    // Category Quick Filter
                    item {
                        CategoryQuickFilter(
                            categories = VaultCategory.values().toList(),
                            selectedCategory = selectedCategory,
                            categoryBreakdown = analytics.categoryBreakdown,
                            onSelect = {
                                selectedCategory = if (selectedCategory == it) null else it
                            }
                        )
                    }

                    // Results Header
                    item {
                        ResultsHeader(
                            totalCount = vaultItems.size,
                            filteredCount = filteredItems.size,
                            searchQuery = searchQuery,
                            selectedCategory = selectedCategory
                        )
                    }

                    // Vault Items
                    items(filteredItems) { item ->
                        EnhancedVaultItemCard(
                            item = item,
                            onClick = { selectedItem = item }
                        )
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = SafeSphereColors.Primary,
            contentColor = Color.White
        ) {
            Text(text = "+", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }

        // Dialogs
        if (showAddDialog) {
            AddVaultItemDialog(
                onDismiss = { showAddDialog = false },
                onSave = { title, content, category ->
                    viewModel.addVaultItem(title, content, category)
                    showAddDialog = false
                }
            )
        }

        selectedItem?.let { item ->
            ViewVaultItemDialog(
                item = item,
                viewModel = viewModel,
                onDismiss = { selectedItem = null }
            )
        }
    }
}

/**
 * Enhanced Vault Header with Real-Time Stats
 */
@Composable
private fun EnhancedVaultHeader(
    analytics: VaultAnalytics,
    onBackClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SafeSphereColors.Surface.copy(alpha = 0.95f),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            SafeSphereColors.Primary.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Text(text = "←", fontSize = 24.sp, color = SafeSphereColors.Primary)
                    }

                    Column {
                        Text(
                            text = "Privacy Vault",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeSphereColors.TextPrimary
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(SafeSphereColors.Success)
                                )
                                Text(
                                    text = "${analytics.totalItems} items • ${formatBytes(analytics.totalSize)}",
                                    fontSize = 12.sp,
                                    color = SafeSphereColors.TextSecondary
                                )
                            }
                        }
                    }
                }

                // Animated lock icon
                val infiniteTransition = rememberInfiniteTransition(label = "lock_pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 0.9f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse"
                )

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    SafeSphereColors.Primary,
                                    SafeSphereColors.Secondary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔐",
                        fontSize = 24.sp,
                        modifier = Modifier.scale(pulseScale)
                    )
                }
            }
        }
    }
}

/**
 * Vault Analytics Dashboard
 */
@Composable
private fun VaultAnalyticsDashboard(analytics: VaultAnalytics) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "📊 Real-Time Analytics",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = SafeSphereColors.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // Stats Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                icon = "📦",
                value = "${analytics.totalItems}",
                label = "Total Items",
                color = SafeSphereColors.Primary,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                icon = "🔒",
                value = "${analytics.encryptedItems}",
                label = "Encrypted",
                color = SafeSphereColors.Success,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                icon = "💾",
                value = formatBytes(analytics.totalSize),
                label = "Storage",
                color = SafeSphereColors.Secondary,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                icon = "🕐",
                value = "${analytics.recentItems}",
                label = "Today",
                color = SafeSphereColors.Accent,
                modifier = Modifier.weight(1f)
            )
        }

        // Encryption Progress Bar
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Encryption Coverage",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SafeSphereColors.TextPrimary
                    )
                    Text(
                        text = "${
                            (analytics.encryptedItems * 100 / analytics.totalItems.coerceAtLeast(
                                1
                            ))
                        }%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeSphereColors.Success
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = analytics.encryptedItems.toFloat() / analytics.totalItems.coerceAtLeast(
                        1
                    ).toFloat(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = SafeSphereColors.Success,
                    trackColor = SafeSphereColors.SurfaceVariant
                )
            }
        }
    }
}

/**
 * Search and Filter Bar
 */
@Composable
private fun SearchAndFilterBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedCategory: VaultCategory?,
    onCategorySelect: (VaultCategory?) -> Unit,
    sortBy: VaultSortOption,
    onSortChange: (VaultSortOption) -> Unit,
    showEncryptedOnly: Boolean,
    onEncryptedToggle: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SafeSphereColors.SurfaceVariant.copy(alpha = 0.5f))
                .border(
                    1.dp,
                    SafeSphereColors.Primary.copy(alpha = 0.3f),
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                textStyle = TextStyle(
                    color = SafeSphereColors.TextPrimary,
                    fontSize = 16.sp
                ),
                cursorBrush = SolidColor(SafeSphereColors.Primary),
                decorationBox = { innerTextField ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🔍", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search vault items...",
                                    fontSize = 16.sp,
                                    color = SafeSphereColors.TextSecondary.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Text(text = "✕", fontSize = 16.sp)
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Sort Dropdown
            var showSortMenu by remember { mutableStateOf(false) }
            Box {
                FilterChip(
                    text = "Sort: ${sortBy.label}",
                    icon = "⬇️",
                    onClick = { showSortMenu = true },
                    isSelected = false
                )

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    VaultSortOption.values().forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                onSortChange(option)
                                showSortMenu = false
                            }
                        )
                    }
                }
            }

            // Encrypted Only Filter
            FilterChip(
                text = "Encrypted Only",
                icon = "🔒",
                onClick = onEncryptedToggle,
                isSelected = showEncryptedOnly
            )
        }
    }
}

/**
 * Category Quick Filter
 */
@Composable
private fun CategoryQuickFilter(
    categories: List<VaultCategory>,
    selectedCategory: VaultCategory?,
    categoryBreakdown: Map<VaultCategory, Int>,
    onSelect: (VaultCategory) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Categories",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = SafeSphereColors.TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val count = categoryBreakdown[category] ?: 0
                CategoryChip(
                    category = category,
                    count = count,
                    isSelected = selectedCategory == category,
                    onClick = { onSelect(category) }
                )
            }
        }
    }
}

/**
 * Results Header
 */
@Composable
private fun ResultsHeader(
    totalCount: Int,
    filteredCount: Int,
    searchQuery: String,
    selectedCategory: VaultCategory?
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = if (searchQuery.isNotEmpty() || selectedCategory != null) {
                "Showing $filteredCount of $totalCount items"
            } else {
                "All Items ($totalCount)"
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = SafeSphereColors.TextSecondary
        )
    }
}

/**
 * Enhanced Vault Item Card
 */
@Composable
private fun EnhancedVaultItemCard(
    item: PrivacyVaultItem,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon with Gradient Background
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                getCategoryColor(item.category).copy(alpha = 0.3f),
                                getCategoryColor(item.category).copy(alpha = 0.1f)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        getCategoryColor(item.category).copy(alpha = 0.4f),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.category.icon,
                    fontSize = 28.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SafeSphereColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.category.displayName,
                        fontSize = 12.sp,
                        color = SafeSphereColors.TextSecondary
                    )

                    Text(text = "•", fontSize = 12.sp, color = SafeSphereColors.TextSecondary)

                    Text(
                        text = formatBytes(item.size.toLong()),
                        fontSize = 12.sp,
                        color = SafeSphereColors.TextSecondary
                    )

                    Text(text = "•", fontSize = 12.sp, color = SafeSphereColors.TextSecondary)

                    Text(
                        text = formatTimeAgo(item.modifiedAt),
                        fontSize = 12.sp,
                        color = SafeSphereColors.TextSecondary
                    )
                }
            }

            // Encryption Badge
            if (item.isEncrypted) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SafeSphereColors.Success.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🔒",
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

/**
 * Enhanced Empty Vault State
 */
@Composable
private fun EnhancedEmptyVaultState(onAddFirstItem: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated Icon
        val infiniteTransition = rememberInfiniteTransition(label = "empty_pulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )

        Text(
            text = "🔐",
            fontSize = 80.sp,
            modifier = Modifier.scale(pulseScale)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Your Vault is Empty",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = SafeSphereColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Start storing sensitive data with\nmilitary-grade AES-256 encryption",
            fontSize = 14.sp,
            color = SafeSphereColors.TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onAddFirstItem,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SafeSphereColors.Primary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(text = "Add First Item", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // What you can store
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "What You Can Store:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                listOf(
                    "🔑 Passwords & PINs",
                    "💳 Credit Card Details",
                    "🆔 ID & Passport Numbers",
                    "📝 Private Notes",
                    "🔐 API Keys & Tokens",
                    "💼 Business Secrets"
                ).forEach { item ->
                    Text(
                        text = item,
                        fontSize = 14.sp,
                        color = SafeSphereColors.TextSecondary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Helper Components
 */
@Composable
private fun StatCard(
    icon: String,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.2f),
                        color.copy(alpha = 0.05f)
                    )
                )
            )
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = icon, fontSize = 24.sp)
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = SafeSphereColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FilterChip(
    text: String,
    icon: String,
    onClick: () -> Unit,
    isSelected: Boolean
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) SafeSphereColors.Primary.copy(alpha = 0.2f)
                else SafeSphereColors.SurfaceVariant
            )
            .border(
                1.dp,
                if (isSelected) SafeSphereColors.Primary
                else SafeSphereColors.TextSecondary.copy(alpha = 0.2f),
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 14.sp)
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) SafeSphereColors.Primary else SafeSphereColors.TextSecondary
            )
        }
    }
}

@Composable
private fun CategoryChip(
    category: VaultCategory,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) getCategoryColor(category).copy(alpha = 0.2f)
                else SafeSphereColors.SurfaceVariant
            )
            .border(
                1.dp,
                if (isSelected) getCategoryColor(category)
                else SafeSphereColors.TextSecondary.copy(alpha = 0.2f),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = category.icon, fontSize = 20.sp)
            Column {
                Text(
                    text = category.displayName,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) getCategoryColor(category) else SafeSphereColors.TextPrimary
                )
                Text(
                    text = "$count items",
                    fontSize = 11.sp,
                    color = SafeSphereColors.TextSecondary
                )
            }
        }
    }
}

/**
 * Data Classes
 */
data class VaultAnalytics(
    val totalItems: Int,
    val encryptedItems: Int,
    val totalSize: Long,
    val recentItems: Int,
    val categoryBreakdown: Map<VaultCategory, Int>,
    val averageItemSize: Long
)

enum class VaultSortOption(val label: String) {
    RECENT("Recent First"),
    OLDEST("Oldest First"),
    NAME_AZ("Name (A-Z)"),
    NAME_ZA("Name (Z-A)"),
    SIZE_DESC("Size (Large)"),
    SIZE_ASC("Size (Small)")
}

/**
 * Helper Functions
 */
private fun getCategoryColor(category: VaultCategory): Color {
    return when (category) {
        VaultCategory.PERSONAL -> SafeSphereColors.Primary
        VaultCategory.FINANCIAL -> SafeSphereColors.Success
        VaultCategory.PASSWORDS -> SafeSphereColors.Accent
        VaultCategory.DOCUMENTS -> SafeSphereColors.Secondary
        VaultCategory.MEDICAL -> SafeSphereColors.Warning
        VaultCategory.NOTES -> SafeSphereColors.Info
        VaultCategory.SCREENSHOTS -> Color(0xFF2196F3) // Blue for screenshots
        VaultCategory.IMAGES -> Color(0xFF4CAF50) // Green for images
        VaultCategory.OTHER -> SafeSphereColors.Error
    }
}

private fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> "${diff / 86400_000}d ago"
    }
}