package com.runanywhere.startup_hackathon20.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.autofill.AutofillManager
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.runanywhere.startup_hackathon20.data.*
import com.runanywhere.startup_hackathon20.security.SecurityManager
import com.runanywhere.startup_hackathon20.viewmodels.SafeSphereViewModel
import kotlinx.coroutines.launch

/**
 * Passwords Screen - Complete Password Manager with Auto-Push Credentials
 */
@Composable
fun PasswordsScreen(viewModel: SafeSphereViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val passwordRepository = remember { PasswordVaultRepository.getInstance(context) }
    val savedPasswords by passwordRepository.passwords.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<PasswordCategory?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedPassword by remember { mutableStateOf<PasswordVaultEntry?>(null) }
    var showAutofillInfo by remember { mutableStateOf(false) }
    var sortBy by remember { mutableStateOf(SortOption.NAME_ASC) }
    var showSortMenu by remember { mutableStateOf(false) }

    val isAutofillEnabled = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val autofillManager = context.getSystemService(AutofillManager::class.java)
            autofillManager?.hasEnabledAutofillServices() == true
        } else {
            false
        }
    }

    val filteredPasswords = remember(savedPasswords, searchQuery, selectedCategory, sortBy) {
        var filtered = savedPasswords

        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter {
                it.service.contains(searchQuery, ignoreCase = true) ||
                        it.username.contains(searchQuery, ignoreCase = true) ||
                        it.url.contains(searchQuery, ignoreCase = true)
            }
        }

        if (selectedCategory != null) {
            filtered = filtered.filter { it.category == selectedCategory }
        }

        // Apply sorting
        when (sortBy) {
            SortOption.NAME_ASC -> filtered.sortedBy { it.service.lowercase() }
            SortOption.NAME_DESC -> filtered.sortedByDescending { it.service.lowercase() }
            SortOption.DATE_NEWEST -> filtered.sortedByDescending { it.createdAt }
            SortOption.DATE_OLDEST -> filtered.sortedBy { it.createdAt }
            SortOption.CATEGORY -> filtered.sortedBy { it.category.displayName }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isAutofillEnabled) {
                AutofillServiceBanner(onEnableClick = { showAutofillInfo = true })
            }

            // Search Bar with Sort Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassCard(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = SafeSphereColors.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = TextStyle(
                                color = SafeSphereColors.TextPrimary,
                                fontSize = 16.sp
                            ),
                            cursorBrush = SolidColor(SafeSphereColors.Primary),
                            decorationBox = { innerTextField ->
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
                            },
                            modifier = Modifier.weight(1f)
                        )

                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear",
                                    tint = SafeSphereColors.TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Sort Button
                Box {
                    GlassCard(
                        modifier = Modifier
                            .clickable { showSortMenu = !showSortMenu }
                            .padding(0.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "⇅", fontSize = 20.sp)
                            Text(
                                text = "Sort",
                                fontSize = 14.sp,
                                color = SafeSphereColors.TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (showSortMenu) {
                        SortMenu(
                            currentSort = sortBy,
                            onSortSelected = {
                                sortBy = it
                                showSortMenu = false
                            },
                            onDismiss = { showSortMenu = false }
                        )
                    }
                }
            }

            // Category Filter (Horizontal)
            CategoryFilterRow(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = if (selectedCategory == it) null else it }
            )

            // Password Count & Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredPasswords.size} password${if (filteredPasswords.size != 1) "s" else ""}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SafeSphereColors.TextPrimary
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Text(
                        text = if (isAutofillEnabled) "✅ Autofill ON" else "⚠️ Autofill OFF",
                        fontSize = 12.sp,
                        color = if (isAutofillEnabled) SafeSphereColors.Success else SafeSphereColors.Warning,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Password List
            if (filteredPasswords.isEmpty()) {
                EmptyPasswordsState(
                    searchQuery = searchQuery,
                    onAddPassword = { showAddDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(
                        top = 8.dp,
                        bottom = 88.dp
                    )
                ) {
                    items(items = filteredPasswords, key = { it.id }) { password ->
                        PasswordCard(
                            password = password,
                            onClick = { selectedPassword = password }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = SafeSphereColors.Primary,
            contentColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add Password",
                modifier = Modifier.size(28.dp)
            )
        }

        if (showAddDialog) {
            AddPasswordDialog(
                repository = passwordRepository,
                onDismiss = { showAddDialog = false },
                onSaved = { showAddDialog = false }
            )
        }

        selectedPassword?.let { password ->
            ViewPasswordDialog(
                password = password,
                onDismiss = { selectedPassword = null }
            )
        }

        if (showAutofillInfo) {
            AutofillSetupDialog(
                onDismiss = { showAutofillInfo = false },
                onOpenSettings = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val intent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE)
                        context.startActivity(intent)
                    }
                    showAutofillInfo = false
                }
            )
        }
    }
}

/**
 * Sort options enum
 */
enum class SortOption(val displayName: String) {
    NAME_ASC("Name (A-Z)"),
    NAME_DESC("Name (Z-A)"),
    DATE_NEWEST("Newest First"),
    DATE_OLDEST("Oldest First"),
    CATEGORY("Category")
}

@Composable
fun SortMenu(
    currentSort: SortOption,
    onSortSelected: (SortOption) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { onDismiss() }
    ) {
        GlassCard(
            modifier = Modifier
                .padding(top = 60.dp, end = 16.dp)
                .align(Alignment.TopEnd)
                .width(180.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                SortOption.values().forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSortSelected(option) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = option.displayName,
                            fontSize = 14.sp,
                            color = if (currentSort == option) SafeSphereColors.Primary
                            else SafeSphereColors.TextPrimary,
                            fontWeight = if (currentSort == option) FontWeight.Bold
                            else FontWeight.Normal
                        )
                        if (currentSort == option) {
                            Text(text = "✓", fontSize = 16.sp, color = SafeSphereColors.Primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AutofillServiceBanner(onEnableClick: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onEnableClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "⚡", fontSize = 32.sp)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Enable Autofill",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary
                )
                Text(
                    text = "Auto-fill passwords in apps and websites",
                    fontSize = 13.sp,
                    color = SafeSphereColors.TextSecondary
                )
            }

            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = "Enable",
                tint = SafeSphereColors.Primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun CategoryFilterRow(
    selectedCategory: PasswordCategory?,
    onCategorySelected: (PasswordCategory) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        // "All" category
        item {
            CategoryChip(
                icon = "📁",
                label = "All",
                isSelected = selectedCategory == null,
                onClick = { onCategorySelected(selectedCategory ?: PasswordCategory.OTHER) }
            )
        }

        // All categories
        items(PasswordCategory.values().size) { index ->
            val category = PasswordCategory.values()[index]
            CategoryChip(
                icon = category.icon,
                label = category.displayName,
                isSelected = selectedCategory == category,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@Composable
fun CategoryChip(
    icon: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) SafeSphereColors.Primary.copy(alpha = 0.2f)
                else SafeSphereColors.Surface.copy(alpha = 0.4f)
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) SafeSphereColors.Primary
                else SafeSphereColors.TextSecondary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = icon, fontSize = 16.sp)
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) SafeSphereColors.Primary
                else SafeSphereColors.TextPrimary
            )
        }
    }
}

@Composable
fun EmptyPasswordsState(
    searchQuery: String,
    onAddPassword: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (searchQuery.isEmpty()) "🔑" else "🔍",
            fontSize = 64.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (searchQuery.isEmpty()) "No passwords saved" else "No matches found",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = SafeSphereColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (searchQuery.isEmpty()) {
                "Add your first password to get started"
            } else {
                "Try a different search query"
            },
            fontSize = 14.sp,
            color = SafeSphereColors.TextSecondary,
            textAlign = TextAlign.Center
        )

        if (searchQuery.isEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))

            GlassButton(
                text = "Add Password",
                onClick = onAddPassword,
                primary = true
            )
        }
    }
}

@Composable
fun PasswordCard(
    password: PasswordVaultEntry,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with category
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        when (password.category) {
                            PasswordCategory.SOCIAL -> Color(0xFF1DA1F2).copy(alpha = 0.2f)
                            PasswordCategory.EMAIL -> Color(0xFFEA4335).copy(alpha = 0.2f)
                            PasswordCategory.BANKING -> Color(0xFF34A853).copy(alpha = 0.2f)
                            PasswordCategory.SHOPPING -> Color(0xFFFBBC05).copy(alpha = 0.2f)
                            PasswordCategory.ENTERTAINMENT -> Color(0xFFE91E63).copy(alpha = 0.2f)
                            PasswordCategory.WORK -> Color(0xFF5E35B1).copy(alpha = 0.2f)
                            PasswordCategory.WEB -> Color(0xFF00ACC1).copy(alpha = 0.2f)
                            PasswordCategory.APP -> Color(0xFF43A047).copy(alpha = 0.2f)
                            else -> SafeSphereColors.Primary.copy(alpha = 0.2f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = password.category.icon, fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Password info
            Column(modifier = Modifier.weight(1f)) {
                // Service name
                Text(
                    text = password.service,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Username
                Text(
                    text = password.username,
                    fontSize = 14.sp,
                    color = SafeSphereColors.TextSecondary,
                    maxLines = 1
                )

                // URL if available
                if (password.url.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "🌐 ${password.url.take(30)}${if (password.url.length > 30) "..." else ""}",
                        fontSize = 11.sp,
                        color = SafeSphereColors.TextSecondary.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Strength bar and category badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Strength indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .height(4.dp)
                                .width(50.dp)
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
                            color = when {
                                password.strengthScore >= 75 -> SafeSphereColors.Success
                                password.strengthScore >= 50 -> SafeSphereColors.Warning
                                else -> SafeSphereColors.Error
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Dot separator
                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = SafeSphereColors.TextSecondary.copy(alpha = 0.5f)
                    )

                    // Category badge
                    Text(
                        text = password.category.displayName,
                        fontSize = 11.sp,
                        color = SafeSphereColors.TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Favorite star or chevron
            if (password.isFavorite) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Favorite",
                    tint = Color(0xFFFBC02D),
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = "›",
                    fontSize = 32.sp,
                    color = SafeSphereColors.TextSecondary.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
fun AddPasswordDialog(
    repository: PasswordVaultRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var service by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(PasswordCategory.OTHER) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Add Password",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Service Name
                OutlinedTextField(
                    value = service,
                    onValueChange = { service = it },
                    label = { Text("Service Name") },
                    placeholder = { Text("e.g., Twitter, Gmail") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SafeSphereColors.Primary,
                        focusedLabelColor = SafeSphereColors.Primary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Username/Email
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username/Email") },
                    placeholder = { Text("Enter username or email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SafeSphereColors.Primary,
                        focusedLabelColor = SafeSphereColors.Primary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    placeholder = { Text("Enter password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None 
                        else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Text(
                                text = if (passwordVisible) "👁" else "🔒",
                                fontSize = 20.sp
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SafeSphereColors.Primary,
                        focusedLabelColor = SafeSphereColors.Primary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // URL (Optional)
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Website/URL (Optional)") },
                    placeholder = { Text("e.g., x.com, twitter.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SafeSphereColors.Primary,
                        focusedLabelColor = SafeSphereColors.Primary
                    )
                )

                // Helper text for URL
                Text(
                    text = "💡 Tip: Enter just the domain name (e.g., 'x.com') for best autofill results",
                    fontSize = 11.sp,
                    color = SafeSphereColors.TextSecondary,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category Selector
                Text(
                    text = "Category",
                    fontSize = 14.sp,
                    color = SafeSphereColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SafeSphereColors.SurfaceVariant)
                        .clickable { showCategoryPicker = !showCategoryPicker }
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = selectedCategory.icon, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = selectedCategory.displayName,
                            color = SafeSphereColors.TextPrimary
                        )
                    }
                }

                if (showCategoryPicker) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SafeSphereColors.SurfaceVariant)
                    ) {
                        PasswordCategory.values().forEach { category ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedCategory = category
                                        showCategoryPicker = false
                                    }
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = category.icon, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = category.displayName,
                                        color = SafeSphereColors.TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Notes (Optional)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("Additional information") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SafeSphereColors.Primary,
                        focusedLabelColor = SafeSphereColors.Primary
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )

                    GlassButton(
                        text = "Save",
                        onClick = {
                            if (service.isNotBlank() && username.isNotBlank() && password.isNotBlank()) {
                                scope.launch {
                                    repository.savePassword(
                                        service = service,
                                        username = username,
                                        password = password,
                                        url = url,
                                        category = selectedCategory,
                                        notes = notes
                                    )
                                    Toast.makeText(context, "Password saved!", Toast.LENGTH_SHORT).show()
                                    onSaved()
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "Please fill required fields",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        primary = true,
                        enabled = service.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun ViewPasswordDialog(password: PasswordVaultEntry, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { PasswordVaultRepository.getInstance(context) }

    var decryptedPassword by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var isAuthenticated by remember { mutableStateOf(false) }
    var showAuthenticationError by remember { mutableStateOf(false) }
    var isAuthenticating by remember { mutableStateOf(false) }

    // Check if biometric is available
    val biometricAvailability = remember {
        com.runanywhere.startup_hackathon20.security.BiometricAuthManager.isBiometricAvailable(context)
    }
    val isBiometricAvailable = biometricAvailability is com.runanywhere.startup_hackathon20.security.BiometricAvailability.Available

    // Trigger biometric authentication on dialog open
    LaunchedEffect(Unit) {
        if (isBiometricAvailable) {
            val activity = context as? androidx.fragment.app.FragmentActivity
            if (activity != null) {
                isAuthenticating = true
                com.runanywhere.startup_hackathon20.security.BiometricAuthManager.authenticate(
                    activity = activity,
                    title = "🔐 Unlock Password",
                    subtitle = "Authenticate to view ${password.service} credentials",
                    negativeButtonText = "Cancel",
                    onSuccess = {
                        isAuthenticating = false
                        isAuthenticated = true
                        // Auto-decrypt password after authentication
                        isLoading = true
                        scope.launch {
                            try {
                                val result = repository.getDecryptedPassword(password.id)
                                result.onSuccess { decrypted ->
                                    decryptedPassword = decrypted.password
                                }
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Failed to decrypt: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    onError = { errorCode, errorMessage ->
                        isAuthenticating = false
                        showAuthenticationError = true
                        onDismiss() // Close dialog on authentication error
                    },
                    onFailed = {
                        isAuthenticating = false
                        showAuthenticationError = true
                        onDismiss() // Close dialog on authentication failure
                    }
                )
            } else {
                // No activity context, allow access without biometric
                isAuthenticated = true
                isLoading = true
                scope.launch {
                    try {
                        val result = repository.getDecryptedPassword(password.id)
                        result.onSuccess { decrypted ->
                            decryptedPassword = decrypted.password
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "Failed to decrypt: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } finally {
                        isLoading = false
                    }
                }
            }
        } else {
            // No biometric available, allow access directly
            isAuthenticated = true
            isLoading = true
            scope.launch {
                try {
                    val result = repository.getDecryptedPassword(password.id)
                    result.onSuccess { decrypted ->
                        decryptedPassword = decrypted.password
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        "Failed to decrypt: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(min = 300.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Show authentication waiting screen
                if (isAuthenticating) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Text(text = "🔐", fontSize = 72.sp)

                        Text(
                            text = "Authentication Required",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeSphereColors.TextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Complete biometric authentication\nto view password details",
                            fontSize = 15.sp,
                            color = SafeSphereColors.TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(SafeSphereColors.Primary.copy(alpha = 0.15f))
                                .border(
                                    width = 1.5.dp,
                                    color = SafeSphereColors.Primary.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "👆 Touch sensor to unlock",
                                fontSize = 14.sp,
                                color = SafeSphereColors.Primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (!isAuthenticated) {
                    // Authentication not yet completed (shouldn't normally show)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = SafeSphereColors.Primary
                        )

                        Text(
                            text = "Initializing...",
                            fontSize = 16.sp,
                            color = SafeSphereColors.TextSecondary
                        )
                    }
                } else {
                    // AUTHENTICATED - Show ENTIRE password card details
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Header with authentication badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = password.service,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SafeSphereColors.TextPrimary
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(text = password.category.icon, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = password.category.displayName,
                                        fontSize = 14.sp,
                                        color = SafeSphereColors.TextSecondary
                                    )
                                }
                            }

                            // Authenticated indicator
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF4CAF50).copy(alpha = 0.25f))
                                    .border(
                                        width = 1.5.dp,
                                        color = Color(0xFF4CAF50),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "✓ Unlocked",
                                    fontSize = 12.sp,
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Username
                        DetailRow(label = "Username", value = password.username, copyable = true)

                        Spacer(modifier = Modifier.height(16.dp))

                        // Password (with show/hide)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Password",
                                    fontSize = 13.sp,
                                    color = SafeSphereColors.TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = SafeSphereColors.Primary
                                    )
                                } else {
                                    Text(
                                        text = if (passwordVisible) decryptedPassword ?: "••••••••"
                                        else "••••••••",
                                        fontSize = 17.sp,
                                        color = SafeSphereColors.TextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = if (!passwordVisible) 2.sp else 0.sp
                                    )
                                }
                            }

                            if (decryptedPassword != null) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Text(
                                            text = if (passwordVisible) "👁" else "🔒",
                                            fontSize = 22.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val clipboard =
                                                context.getSystemService(Context.CLIPBOARD_SERVICE)
                                                        as ClipboardManager
                                            val clip =
                                                ClipData.newPlainText("password", decryptedPassword)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(
                                                context,
                                                "Password copied to clipboard",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    ) {
                                        Text(text = "📋", fontSize = 22.sp)
                                    }
                                }
                            }
                        }

                        // Password strength indicator
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(6.dp)
                                    .width(100.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(SafeSphereColors.SurfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(password.strengthScore / 100f)
                                        .background(
                                            when {
                                                password.strengthScore >= 75 -> Color(0xFF4CAF50)
                                                password.strengthScore >= 50 -> Color(0xFFFBC02D)
                                                else -> Color(0xFFF44336)
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
                                fontSize = 12.sp,
                                color = when {
                                    password.strengthScore >= 75 -> Color(0xFF4CAF50)
                                    password.strengthScore >= 50 -> Color(0xFFFBC02D)
                                    else -> Color(0xFFF44336)
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (password.url.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            DetailRow(
                                label = "Website",
                                value = password.url,
                                copyable = true,
                                isUrl = true
                            )
                        }

                        if (password.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Column {
                                Text(
                                    text = "Notes",
                                    fontSize = 13.sp,
                                    color = SafeSphereColors.TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = password.notes,
                                    fontSize = 14.sp,
                                    color = SafeSphereColors.TextPrimary,
                                    lineHeight = 20.sp
                                )
                            }
                        }

                        // Created date
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Created: ${
                                java.text.SimpleDateFormat(
                                    "MMM dd, yyyy",
                                    java.util.Locale.getDefault()
                                ).format(password.createdAt)
                            }",
                            fontSize = 12.sp,
                            color = SafeSphereColors.TextSecondary
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Delete and Close buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            GlassButton(
                                text = "Delete",
                                onClick = {
                                    scope.launch {
                                        repository.deletePassword(password.id)
                                        Toast.makeText(
                                            context,
                                            "Password deleted",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onDismiss()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )

                            GlassButton(
                                text = "Close",
                                onClick = onDismiss,
                                primary = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AutofillSetupDialog(onDismiss: () -> Unit, onOpenSettings: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Enable Autofill",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "To auto-fill passwords in apps and sites, turn on the SafeSphere autofill service in your device settings.",
                    fontSize = 15.sp,
                    color = SafeSphereColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    GlassButton(
                        text = "Open Settings",
                        onClick = onOpenSettings,
                        primary = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    copyable: Boolean = false,
    isUrl: Boolean = false
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = SafeSphereColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (isUrl) {
                Text(
                    text = value,
                    fontSize = 14.sp,
                    color = SafeSphereColors.Primary,
                    modifier = Modifier
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(value))
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        }
                )
            } else {
                Text(
                    text = value,
                    fontSize = 14.sp,
                    color = SafeSphereColors.TextPrimary
                )
            }
        }
        if (copyable) {
            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                            as ClipboardManager
                    val clip = ClipData.newPlainText(label, value)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                }
            ) {
                Text(
                    text = "📋",
                    fontSize = 20.sp
                )
            }
        }
    }
}

private fun getCategoryName(category: PasswordCategory): String {
    return "${category.icon} ${category.displayName}"
}
