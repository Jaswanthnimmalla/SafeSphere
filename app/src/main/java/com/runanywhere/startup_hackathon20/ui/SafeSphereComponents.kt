package com.runanywhere.startup_hackathon20.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.runanywhere.startup_hackathon20.data.VaultCategory

/**
 * Glass-morphism Card Component
 * Semi-transparent card with border and subtle backdrop blur effect
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SafeSphereColors.Surface.copy(alpha = 0.6f))
            .border(
                width = 1.5.dp,
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(
                        SafeSphereColors.Primary.copy(alpha = 0.3f),
                        SafeSphereColors.Secondary.copy(alpha = 0.3f),
                        SafeSphereColors.Accent.copy(alpha = 0.3f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        content()
    }
}

/**
 * Glass Button Component
 * Primary or secondary button with glass effect
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true
) {
    val backgroundColor = if (primary) {
        // Vibrant gradient background for primary buttons
        Brush.linearGradient(
            colors = listOf(
                SafeSphereColors.Primary,
                SafeSphereColors.Primary.copy(alpha = 0.85f)
            )
        )
    } else {
        // Solid background for secondary buttons with better visibility
        Brush.linearGradient(
            colors = listOf(
                SafeSphereColors.Surface.copy(alpha = 0.9f),
                SafeSphereColors.SurfaceVariant.copy(alpha = 0.8f)
            )
        )
    }

    val borderColor = if (primary) {
        SafeSphereColors.Primary.copy(alpha = 0.6f)
    } else {
        SafeSphereColors.Primary.copy(alpha = 0.3f)
    }

    val textColor = if (primary) {
        Color.White
    } else {
        if (enabled) SafeSphereColors.Primary else SafeSphereColors.TextSecondary
    }

    Box(
        modifier = modifier
            .heightIn(min = 50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .clickable(enabled = enabled, onClick = onClick)
            .border(
                width = if (primary) 2.dp else 1.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = if (primary) FontWeight.Bold else FontWeight.SemiBold,
            color = textColor,
            letterSpacing = 0.3.sp,
            maxLines = 1
        )
    }
}

/**
 * SafeSphere Header Component
 * Header bar with title, subtitle, and optional action buttons
 */
@Composable
fun SafeSphereHeader(
    title: String,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    onActionClick: (() -> Unit)? = null,
    actionIcon: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SafeSphereColors.Surface.copy(alpha = 0.8f))
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Back button
                onBackClick?.let {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SafeSphereColors.SurfaceVariant)
                            .clickable(onClick = it),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "←",
                            fontSize = 24.sp,
                            color = SafeSphereColors.TextPrimary
                        )
                    }
                }

                // Title and subtitle
                Column {
                    Text(
                        text = title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeSphereColors.TextPrimary
                    )

                    subtitle?.let {
                        Text(
                            text = it,
                            fontSize = 14.sp,
                            color = SafeSphereColors.TextSecondary
                        )
                    }
                }
            }

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                onActionClick?.let {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SafeSphereColors.Primary.copy(alpha = 0.8f))
                            .clickable(onClick = it),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = actionIcon ?: "+",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                onSettingsClick?.let {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SafeSphereColors.SurfaceVariant)
                            .clickable(onClick = it),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⚙",
                            fontSize = 20.sp,
                            color = SafeSphereColors.TextPrimary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Add Vault Item Dialog
 */
@Composable
fun AddVaultItemDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, category: VaultCategory) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(VaultCategory.NOTES) }
    var showCategoryPicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Add Encrypted Item",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Title field
                Text(
                    text = "Title",
                    fontSize = 14.sp,
                    color = SafeSphereColors.TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = TextStyle(
                        color = SafeSphereColors.TextPrimary,
                        fontSize = 16.sp
                    ),
                    cursorBrush = SolidColor(SafeSphereColors.Primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SafeSphereColors.SurfaceVariant)
                        .padding(16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Category picker
                Text(
                    text = "Category",
                    fontSize = 14.sp,
                    color = SafeSphereColors.TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SafeSphereColors.SurfaceVariant)
                        .clickable { showCategoryPicker = !showCategoryPicker }
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = selectedCategory.icon,
                            fontSize = 20.sp
                        )
                        Text(
                            text = selectedCategory.displayName,
                            fontSize = 16.sp,
                            color = SafeSphereColors.TextPrimary
                        )
                    }
                }

                // Category picker dropdown
                if (showCategoryPicker) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SafeSphereColors.SurfaceVariant)
                            .padding(8.dp)
                    ) {
                        VaultCategory.values().forEach { category ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedCategory = category
                                        showCategoryPicker = false
                                    }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = category.icon,
                                        fontSize = 20.sp
                                    )
                                    Text(
                                        text = category.displayName,
                                        fontSize = 14.sp,
                                        color = SafeSphereColors.TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content field
                Text(
                    text = "Content (will be encrypted)",
                    fontSize = 14.sp,
                    color = SafeSphereColors.TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                BasicTextField(
                    value = content,
                    onValueChange = { content = it },
                    textStyle = TextStyle(
                        color = SafeSphereColors.TextPrimary,
                        fontSize = 16.sp
                    ),
                    cursorBrush = SolidColor(SafeSphereColors.Primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SafeSphereColors.SurfaceVariant)
                        .padding(16.dp)
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
                            if (title.isNotBlank() && content.isNotBlank()) {
                                onSave(title, content, selectedCategory)
                            }
                        },
                        primary = true,
                        enabled = title.isNotBlank() && content.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * View Vault Item Dialog
 */
@Composable
fun ViewVaultItemDialog(
    item: com.runanywhere.startup_hackathon20.data.PrivacyVaultItem,
    viewModel: com.runanywhere.startup_hackathon20.viewmodels.SafeSphereViewModel,
    onDismiss: () -> Unit
) {
    var decryptedContent by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isAuthenticating by remember { mutableStateOf(true) }
    var authenticationFailed by remember { mutableStateOf(false) }

    // Get activity context (now it's FragmentActivity)
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity

    // Biometric authentication before decrypting
    LaunchedEffect(item.id) {
        if (activity != null) {
            // Check if biometric is enabled
            val isBiometricEnabled = context.getSharedPreferences(
                "safesphere_prefs",
                android.content.Context.MODE_PRIVATE
            )
                .getBoolean("biometric_enabled", false)

            if (isBiometricEnabled) {
                // Authenticate with biometric first
                com.runanywhere.startup_hackathon20.security.BiometricAuthManager.authenticate(
                    activity = activity,
                    title = "Unlock Vault Item",
                    subtitle = "Authenticate to view ${item.title}",
                    negativeButtonText = "Cancel",
                    onSuccess = {
                        // Biometric success - decrypt content
                        isAuthenticating = false
                        isLoading = true
                        viewModel.getDecryptedItem(item.id) { decryptResult ->
                            isLoading = false
                            decryptResult.onSuccess { decrypted ->
                                decryptedContent = decrypted.content
                            }.onFailure { e ->
                                error = e.message
                            }
                        }
                    },
                    onError = { errorCode, errorString ->
                        // Biometric error
                        isAuthenticating = false
                        authenticationFailed = true
                        error = errorString
                    },
                    onFailed = {
                        // Biometric failed
                        isAuthenticating = false
                        authenticationFailed = true
                        error = "Authentication failed"
                    }
                )
            } else {
                // No biometric - decrypt directly
                isAuthenticating = false
                isLoading = true
                viewModel.getDecryptedItem(item.id) { decryptResult ->
                    isLoading = false
                    decryptResult.onSuccess { decrypted ->
                        decryptedContent = decrypted.content
                    }.onFailure { e ->
                        error = e.message
                    }
                }
            }
        } else {
            // No activity context - decrypt directly
            isAuthenticating = false
            isLoading = true
            viewModel.getDecryptedItem(item.id) { decryptResult ->
                isLoading = false
                decryptResult.onSuccess { decrypted ->
                    decryptedContent = decrypted.content
                }.onFailure { e ->
                    error = e.message
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeSphereColors.TextPrimary
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = item.category.icon,
                                fontSize = 16.sp
                            )
                            Text(
                                text = item.category.displayName,
                                fontSize = 14.sp,
                                color = SafeSphereColors.TextSecondary
                            )
                        }
                    }

                    Text(
                        text = "🔒",
                        fontSize = 24.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Content
                when {
                    isAuthenticating -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "🔐",
                                    fontSize = 56.sp
                                )
                                Text(
                                    text = "Authentication Required",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SafeSphereColors.TextPrimary
                                )
                                Text(
                                    text = "Please unlock with fingerprint or PIN",
                                    fontSize = 13.sp,
                                    color = SafeSphereColors.TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = SafeSphereColors.Primary
                                )
                                Text(
                                    text = "Decrypting...",
                                    fontSize = 14.sp,
                                    color = SafeSphereColors.TextSecondary
                                )
                            }
                        }
                    }

                    authenticationFailed || error != null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "❌",
                                fontSize = 48.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = error ?: "Authentication failed",
                                fontSize = 14.sp,
                                color = SafeSphereColors.Error,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    else -> {
                        Column {
                            Text(
                                text = "Decrypted Content",
                                fontSize = 14.sp,
                                color = SafeSphereColors.TextSecondary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SafeSphereColors.SurfaceVariant)
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = decryptedContent ?: "",
                                    fontSize = 16.sp,
                                    color = SafeSphereColors.TextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!isAuthenticating && !isLoading) {
                        if (decryptedContent != null) {
                            GlassButton(
                                text = "Delete",
                                onClick = {
                                    viewModel.deleteVaultItem(item.id)
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

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
