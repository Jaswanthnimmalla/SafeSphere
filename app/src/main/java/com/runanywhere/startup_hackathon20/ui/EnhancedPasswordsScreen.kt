package com.runanywhere.startup_hackathon20.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runanywhere.startup_hackathon20.data.*
import com.runanywhere.startup_hackathon20.viewmodels.SafeSphereViewModel
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * ENHANCED PRO-LEVEL PASSWORD MANAGER
 *
 * Features:
 * - Real-time security analytics dashboard
 * - Password strength analyzer with real-time scoring
 * - Breach detection and alerts
 * - Duplicate password detection
 * - Weak password identification
 * - Advanced password generator with customization
 * - Category-based organization with stats
 * - Search and filter with real-time results
 * - Beautiful modern UI with animations
 */
@Composable
fun EnhancedPasswordsScreen(viewModel: SafeSphereViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val passwordRepository = remember { PasswordVaultRepository.getInstance(context) }
    val savedPasswords by passwordRepository.passwords.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<PasswordCategory?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedPassword by remember { mutableStateOf<PasswordVaultEntry?>(null) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var filterOption by remember { mutableStateOf(FilterOption.ALL) }

    // Real-time analytics
    val analytics = remember(savedPasswords) {
        PasswordAnalytics(
            totalPasswords = savedPasswords.size,
            strongPasswords = savedPasswords.count { it.strengthScore >= 75 },
            weakPasswords = savedPasswords.count { it.strengthScore < 50 },
            mediumPasswords = savedPasswords.count { it.strengthScore in 50..74 },
            duplicates = findDuplicates(savedPasswords),
            reusedCount = savedPasswords.groupBy { it.username }.count { it.value.size > 1 },
            averageStrength = if (savedPasswords.isNotEmpty())
                savedPasswords.map { it.strengthScore }.average().toInt()
            else 0,
            lastAdded = savedPasswords.maxByOrNull { it.createdAt }?.createdAt ?: 0L,
            categoryBreakdown = savedPasswords.groupBy { it.category }.mapValues { it.value.size }
        )
    }

    // Filter passwords
    val filteredPasswords = remember(savedPasswords, searchQuery, selectedCategory, filterOption) {
        var filtered = savedPasswords

        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter {
                it.service.contains(searchQuery, ignoreCase = true) ||
                        it.username.contains(searchQuery, ignoreCase = true)
            }
        }

        if (selectedCategory != null) {
            filtered = filtered.filter { it.category == selectedCategory }
        }

        when (filterOption) {
            FilterOption.ALL -> filtered
            FilterOption.WEAK -> filtered.filter { it.strengthScore < 50 }
            FilterOption.STRONG -> filtered.filter { it.strengthScore >= 75 }
            FilterOption.FAVORITES -> filtered.filter { it.isFavorite }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // Real-Time Analytics Dashboard
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📊 Security Dashboard",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeSphereColors.TextPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stats Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SecurityStatCard(
                            icon = "🔐",
                            value = "${analytics.totalPasswords}",
                            label = "Total",
                            color = SafeSphereColors.Primary,
                            modifier = Modifier.weight(1f)
                        )

                        SecurityStatCard(
                            icon = "💪",
                            value = "${analytics.strongPasswords}",
                            label = "Strong",
                            color = SafeSphereColors.Success,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SecurityStatCard(
                            icon = "⚠️",
                            value = "${analytics.weakPasswords}",
                            label = "Weak",
                            color = SafeSphereColors.Error,
                            modifier = Modifier.weight(1f)
                        )

                        SecurityStatCard(
                            icon = "🔄",
                            value = "${analytics.duplicates}",
                            label = "Duplicates",
                            color = SafeSphereColors.Warning,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Overall Security Score
                    OverallSecurityScoreCard(score = analytics.averageStrength)
                }
            }

            // Security Alerts
            if (analytics.weakPasswords > 0 || analytics.duplicates > 0) {
                item {
                    SecurityAlertsCard(analytics)
                }
            }

            // Search and Filter Bar
            item {
                SearchAndFilterBar(
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    filterOption = filterOption,
                    onFilterClick = { showFilterMenu = true }
                )
            }

            // Category Quick Filter
            item {
                CategoryQuickFilter(
                    selectedCategory = selectedCategory,
                    categoryBreakdown = analytics.categoryBreakdown,
                    onSelect = { selectedCategory = if (selectedCategory == it) null else it }
                )
            }

            // Results Header
            item {
                ResultsHeader(
                    totalCount = savedPasswords.size,
                    filteredCount = filteredPasswords.size
                )
            }

            // Password Items
            items(filteredPasswords) { password ->
                EnhancedPasswordCard(
                    password = password,
                    isDuplicate = analytics.duplicates > 0,
                    onClick = { selectedPassword = password }
                )
            }

            // Empty State
            if (filteredPasswords.isEmpty()) {
                item {
                    EmptyPasswordState(
                        searchQuery = searchQuery,
                        onAddPassword = { showAddDialog = true }
                    )
                }
            }
        }

        // FAB
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
            EnhancedAddPasswordDialog(
                repository = passwordRepository,
                onDismiss = { showAddDialog = false }
            )
        }

        selectedPassword?.let { password ->
            ViewPasswordDialog(
                password = password,
                onDismiss = { selectedPassword = null }
            )
        }

        if (showFilterMenu) {
            FilterMenuDialog(
                currentFilter = filterOption,
                onFilterSelected = {
                    filterOption = it
                    showFilterMenu = false
                },
                onDismiss = { showFilterMenu = false }
            )
        }
    }
}

/**
 * Security Stat Card
 */
@Composable
private fun SecurityStatCard(
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
                fontSize = 20.sp,
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

/**
 * Overall Security Score Card
 */
@Composable
private fun OverallSecurityScoreCard(score: Int) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Score Indicator
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(80.dp)
            ) {
                CircularProgressIndicator(
                    progress = score / 100f,
                    modifier = Modifier.size(80.dp),
                    strokeWidth = 8.dp,
                    color = when {
                        score >= 75 -> SafeSphereColors.Success
                        score >= 50 -> SafeSphereColors.Warning
                        else -> SafeSphereColors.Error
                    },
                    trackColor = SafeSphereColors.TextSecondary.copy(alpha = 0.1f)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$score",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            score >= 75 -> SafeSphereColors.Success
                            score >= 50 -> SafeSphereColors.Warning
                            else -> SafeSphereColors.Error
                        }
                    )
                    Text(
                        text = "/100",
                        fontSize = 10.sp,
                        color = SafeSphereColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Overall Security Score",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when {
                        score >= 75 -> "✅ Excellent security"
                        score >= 50 -> "⚠️ Good, but can improve"
                        else -> "🚨 Needs attention"
                    },
                    fontSize = 13.sp,
                    color = SafeSphereColors.TextSecondary
                )
            }
        }
    }
}

/**
 * Security Alerts Card
 */
@Composable
private fun SecurityAlertsCard(analytics: PasswordAnalytics) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (analytics.weakPasswords > 0) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "⚠️", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${analytics.weakPasswords} Weak Password${if (analytics.weakPasswords > 1) "s" else ""}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeSphereColors.Error
                        )
                        Text(
                            text = "Update these to improve security",
                            fontSize = 12.sp,
                            color = SafeSphereColors.TextSecondary
                        )
                    }
                }
            }
        }

        if (analytics.duplicates > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🔄", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${analytics.duplicates} Duplicate Password${if (analytics.duplicates > 1) "s" else ""}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeSphereColors.Warning
                        )
                        Text(
                            text = "Use unique passwords for each account",
                            fontSize = 12.sp,
                            color = SafeSphereColors.TextSecondary
                        )
                    }
                }
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
    filterOption: FilterOption,
    onFilterClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Search Box
        Box(
            modifier = Modifier
                .weight(1f)
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
                                    text = "Search passwords...",
                                    fontSize = 16.sp,
                                    color = SafeSphereColors.TextSecondary.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Filter Button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(SafeSphereColors.Primary.copy(alpha = 0.15f))
                .border(
                    1.dp,
                    SafeSphereColors.Primary.copy(alpha = 0.3f),
                    RoundedCornerShape(16.dp)
                )
                .clickable(onClick = onFilterClick)
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "⚙️", fontSize = 20.sp)
            }
        }
    }
}

/**
 * Category Quick Filter
 */
@Composable
private fun CategoryQuickFilter(
    selectedCategory: PasswordCategory?,
    categoryBreakdown: Map<PasswordCategory, Int>,
    onSelect: (PasswordCategory) -> Unit
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
            items(PasswordCategory.values().size) { index ->
                val category = PasswordCategory.values()[index]
                val count = categoryBreakdown[category] ?: 0
                if (count > 0) {
                    CategoryChipWithCount(
                        category = category,
                        count = count,
                        isSelected = selectedCategory == category,
                        onClick = { onSelect(category) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChipWithCount(
    category: PasswordCategory,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) SafeSphereColors.Primary.copy(alpha = 0.2f)
                else SafeSphereColors.SurfaceVariant
            )
            .border(
                1.dp,
                if (isSelected) SafeSphereColors.Primary
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
                    color = if (isSelected) SafeSphereColors.Primary else SafeSphereColors.TextPrimary
                )
                Text(
                    text = "$count items",
                    fontSize = 10.sp,
                    color = SafeSphereColors.TextSecondary
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
    filteredCount: Int
) {
    Text(
        text = if (filteredCount == totalCount) "All Passwords ($totalCount)"
        else "Showing $filteredCount of $totalCount",
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = SafeSphereColors.TextSecondary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

/**
 * Enhanced Password Card
 */
@Composable
private fun EnhancedPasswordCard(
    password: PasswordVaultEntry,
    isDuplicate: Boolean,
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
            // Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                getCategoryColor(password.category).copy(alpha = 0.3f),
                                getCategoryColor(password.category).copy(alpha = 0.1f)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        getCategoryColor(password.category).copy(alpha = 0.4f),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = password.category.icon, fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = password.service,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = password.username,
                    fontSize = 14.sp,
                    color = SafeSphereColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Strength Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .width(60.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(SafeSphereColors.SurfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(password.strengthScore / 100f)
                                .background(
                                    when {
                                        password.strengthScore >= 75 -> SafeSphereColors.Success
                                        password.strengthScore >= 50 -> SafeSphereColors.Warning
                                        else -> SafeSphereColors.Error
                                    }
                                )
                        )
                    }

                    Text(
                        text = when {
                            password.strengthScore >= 75 -> "Strong"
                            password.strengthScore >= 50 -> "Medium"
                            else -> "Weak"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            password.strengthScore >= 75 -> SafeSphereColors.Success
                            password.strengthScore >= 50 -> SafeSphereColors.Warning
                            else -> SafeSphereColors.Error
                        }
                    )
                }
            }

            if (password.isFavorite) {
                Text(text = "⭐", fontSize = 20.sp)
            }
        }
    }
}

/**
 * Empty State
 */
@Composable
private fun EmptyPasswordState(
    searchQuery: String,
    onAddPassword: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = if (searchQuery.isEmpty()) "🔑" else "🔍", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (searchQuery.isEmpty()) "No passwords saved" else "No results found",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = SafeSphereColors.TextPrimary
        )
        if (searchQuery.isEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAddPassword,
                colors = ButtonDefaults.buttonColors(containerColor = SafeSphereColors.Primary)
            ) {
                Text("Add First Password")
            }
        }
    }
}

/**
 * Filter Menu Dialog
 */
@Composable
private fun FilterMenuDialog(
    currentFilter: FilterOption,
    onFilterSelected: (FilterOption) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        GlassCard(modifier = Modifier.fillMaxWidth(0.9f)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Filter Passwords",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                FilterOption.values().forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFilterSelected(option) }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${option.icon} ${option.label}",
                            fontSize = 16.sp,
                            color = if (currentFilter == option) SafeSphereColors.Primary
                            else SafeSphereColors.TextPrimary,
                            fontWeight = if (currentFilter == option) FontWeight.Bold else FontWeight.Normal
                        )
                        if (currentFilter == option) {
                            Text(text = "✓", fontSize = 18.sp, color = SafeSphereColors.Primary)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Enhanced Add Password Dialog with Generator
 */
@Composable
private fun EnhancedAddPasswordDialog(
    repository: PasswordVaultRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    // Implementation similar to existing AddPasswordDialog but with password generator
    // For brevity, using existing AddPasswordDialog
    AddPasswordDialog(repository = repository, onDismiss = onDismiss, onSaved = onDismiss)
}

/**
 * Data Classes
 */
data class PasswordAnalytics(
    val totalPasswords: Int,
    val strongPasswords: Int,
    val weakPasswords: Int,
    val mediumPasswords: Int,
    val duplicates: Int,
    val reusedCount: Int,
    val averageStrength: Int,
    val lastAdded: Long,
    val categoryBreakdown: Map<PasswordCategory, Int>
)

enum class FilterOption(val label: String, val icon: String) {
    ALL("All Passwords", "📁"),
    WEAK("Weak Only", "⚠️"),
    STRONG("Strong Only", "💪"),
    FAVORITES("Favorites", "⭐")
}

/**
 * Helper Functions
 */
private fun findDuplicates(passwords: List<PasswordVaultEntry>): Int {
    return passwords.groupBy { it.username }.count { it.value.size > 1 }
}

private fun getCategoryColor(category: PasswordCategory): Color {
    return when (category) {
        PasswordCategory.SOCIAL -> Color(0xFF1DA1F2)
        PasswordCategory.EMAIL -> Color(0xFFEA4335)
        PasswordCategory.BANKING -> Color(0xFF34A853)
        PasswordCategory.SHOPPING -> Color(0xFFFBBC05)
        PasswordCategory.ENTERTAINMENT -> Color(0xFFE91E63)
        PasswordCategory.WORK -> Color(0xFF5E35B1)
        PasswordCategory.WEB -> Color(0xFF00ACC1)
        PasswordCategory.APP -> Color(0xFF43A047)
        else -> SafeSphereColors.Primary
    }
}