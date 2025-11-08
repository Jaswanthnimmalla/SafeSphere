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

    val isAutofillEnabled = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val autofillManager = context.getSystemService(AutofillManager::class.java)
            autofillManager?.hasEnabledAutofillServices() == true
        } else {
            false
        }
    }

    val filteredPasswords = remember(savedPasswords, searchQuery, selectedCategory) {
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

        filtered
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

            // Search Bar
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
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

            // Category Filter
            CategoryFilterRow(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = if (selectedCategory == it) null else it }
            )

            // Password Count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredPasswords.size} passwords",
                    fontSize = 14.sp,
                    color = SafeSphereColors.TextSecondary
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
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        val categories = PasswordCategory.values()

        items(categories.size) { index ->
            val category = categories[index]
            val isSelected = selectedCategory == category

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) SafeSphereColors.Primary.copy(alpha = 0.15f)
                        else SafeSphereColors.Surface.copy(alpha = 0.6f)
                    )
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) SafeSphereColors.Primary
                        else SafeSphereColors.TextSecondary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onCategorySelected(category) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = category.icon, fontSize = 18.sp)
                    Text(
                        text = category.displayName,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) SafeSphereColors.Primary
                        else SafeSphereColors.TextPrimary
                    )
                }
            }
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
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SafeSphereColors.Primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = password.category.icon, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = password.service,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SafeSphereColors.TextPrimary
                )

                Text(
                    text = password.username,
                    fontSize = 13.sp,
                    color = SafeSphereColors.TextSecondary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .width(40.dp)
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
                        fontSize = 10.sp,
                        color = when {
                            password.strengthScore >= 75 -> SafeSphereColors.Success
                            password.strengthScore >= 50 -> SafeSphereColors.Warning
                            else -> SafeSphereColors.Error
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (password.isFavorite) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Favorite",
                    tint = Color(0xFFFBC02D),
                    modifier = Modifier.size(20.dp)
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
    var requiresBiometric by remember { mutableStateOf(true) }
    var biometricAuthenticated by remember { mutableStateOf(false) }

    // Check if biometric is available
    val biometricAvailability = remember {
        com.runanywhere.startup_hackathon20.security.BiometricAuthManager.isBiometricAvailable(context)
    }
    val isBiometricAvailable = biometricAvailability is com.runanywhere.startup_hackathon20.security.BiometricAvailability.Available

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
                // Header
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
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Username
                DetailRow(label = "Username", value = password.username, copyable = true)

                Spacer(modifier = Modifier.height(12.dp))

                // Password
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Password",
                            fontSize = 12.sp,
                            color = SafeSphereColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        if (decryptedPassword == null && !isLoading) {
                            TextButton(
                                onClick = {
                                    // Check if biometric authentication is required and available
                                    if (isBiometricAvailable && requiresBiometric && !biometricAuthenticated) {
                                        // Authenticate with biometrics
                                        val activity = context as? androidx.fragment.app.FragmentActivity
                                        if (activity != null) {
                                            com.runanywhere.startup_hackathon20.security.BiometricAuthManager.authenticate(
                                                activity = activity,
                                                title = "Unlock Password",
                                                subtitle = "Authenticate to view ${password.service} password",
                                                negativeButtonText = "Cancel",
                                                onSuccess = {
                                                    biometricAuthenticated = true
                                                    // Now decrypt the password
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
                                                    Toast.makeText(
                                                        context,
                                                        "Authentication failed: $errorMessage",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                },
                                                onFailed = {
                                                    Toast.makeText(
                                                        context,
                                                        "Authentication failed. Please try again.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            )
                                        }
                                    } else {
                                        // No biometric or already authenticated, decrypt directly
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
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(if (isBiometricAvailable && requiresBiometric) "🔐" else "🔓")
                                    Text(
                                        if (isBiometricAvailable && requiresBiometric) "Unlock with Biometric"
                                        else "Reveal Password"
                                    )
                                }
                            }
                        } else if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = SafeSphereColors.Primary
                            )
                        } else {
                            Text(
                                text = if (passwordVisible) decryptedPassword ?: "••••••••" 
                                    else "••••••••",
                                fontSize = 16.sp,
                                color = SafeSphereColors.TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (decryptedPassword != null) {
                        Row {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Text(
                                    text = if (passwordVisible) "👁" else "🔒",
                                    fontSize = 20.sp
                                )
                            }
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) 
                                        as ClipboardManager
                                    val clip = ClipData.newPlainText("password", decryptedPassword)
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

                // Biometric status indicator
                if (isBiometricAvailable && decryptedPassword == null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "🔐", fontSize = 12.sp)
                        Text(
                            text = "Biometric authentication required",
                            fontSize = 11.sp,
                            color = SafeSphereColors.Primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else if (!isBiometricAvailable && decryptedPassword == null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ℹ️ Biometric not available on this device",
                        fontSize = 11.sp,
                        color = SafeSphereColors.TextSecondary
                    )
                }

                if (password.url.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    DetailRow(label = "URL", value = password.url, copyable = true, isUrl = true)
                }

                if (password.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column {
                        Text(
                            text = "Notes",
                            fontSize = 12.sp,
                            color = SafeSphereColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = password.notes,
                            fontSize = 14.sp,
                            color = SafeSphereColors.TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

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
                                Toast.makeText(context, "Password deleted", Toast.LENGTH_SHORT).show()
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
