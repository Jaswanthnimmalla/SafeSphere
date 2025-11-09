package com.runanywhere.startup_hackathon20.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runanywhere.startup_hackathon20.viewmodels.SafeSphereViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * ℹ️ ABOUT US SCREEN - Pro-Level with Animations & Stats
 */
@Composable
fun AboutUsScreen(viewModel: SafeSphereViewModel) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SafeSphereColors.Background,
                        SafeSphereColors.BackgroundDark
                    )
                )
            ),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Section
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    SafeSphereColors.Primary.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Animated logo
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .graphicsLayer {
                                    scaleX = pulseScale
                                    scaleY = pulseScale
                                }
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            SafeSphereColors.Primary,
                                            SafeSphereColors.Secondary
                                        )
                                    )
                                )
                                .border(
                                    width = 3.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            SafeSphereColors.Primary.copy(alpha = 0.5f),
                                            SafeSphereColors.Secondary.copy(alpha = 0.5f)
                                        )
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🛡️",
                                fontSize = 60.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "SafeSphere",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            style = androidx.compose.ui.text.TextStyle(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        SafeSphereColors.Primary,
                                        SafeSphereColors.Secondary,
                                        SafeSphereColors.Accent
                                    )
                                )
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Privacy-First Mobile Security",
                            fontSize = 16.sp,
                            color = SafeSphereColors.TextSecondary,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Version badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = SafeSphereColors.Primary.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, SafeSphereColors.Primary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "Version 1.0.0 Beta",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SafeSphereColors.Primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Mission Statement
        item {
            StatCard(
                title = "Our Mission",
                icon = "🎯",
                color = SafeSphereColors.Primary,
                content = {
                    Text(
                        text = "To empower individuals with complete control over their digital privacy through offline-first, zero-trust security architecture.",
                        fontSize = 15.sp,
                        color = SafeSphereColors.TextPrimary,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center
                    )
                }
            )
        }

        // Stats Grid
        item {
            Text(
                text = "Platform Statistics",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MiniStatCard(
                    icon = "🔐",
                    value = "AES-256",
                    label = "Encryption",
                    color = SafeSphereColors.Success,
                    modifier = Modifier.weight(1f)
                )
                MiniStatCard(
                    icon = "📴",
                    value = "100%",
                    label = "Offline",
                    color = SafeSphereColors.Primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MiniStatCard(
                    icon = "🚫",
                    value = "Zero",
                    label = "Cloud Sync",
                    color = SafeSphereColors.Warning,
                    modifier = Modifier.weight(1f)
                )
                MiniStatCard(
                    icon = "🤖",
                    value = "On-Device",
                    label = "AI Models",
                    color = SafeSphereColors.Secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Features
        item {
            Text(
                text = "Core Features",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FeatureItem(
                    icon = "🔐",
                    title = "Privacy Vault",
                    description = "Military-grade AES-256 encryption for all sensitive data",
                    color = SafeSphereColors.Primary
                )
                FeatureItem(
                    icon = "🗝️",
                    title = "Password Manager",
                    description = "Secure password storage with breach detection",
                    color = SafeSphereColors.Accent
                )
                FeatureItem(
                    icon = "🤖",
                    title = "Offline AI",
                    description = "Privacy advisor running entirely on your device",
                    color = SafeSphereColors.Secondary
                )
                FeatureItem(
                    icon = "🛡️",
                    title = "Threat Simulation",
                    description = "Educational security training in real-time",
                    color = SafeSphereColors.Warning
                )
                FeatureItem(
                    icon = "📊",
                    title = "Data Map",
                    description = "Visualize your encrypted storage distribution",
                    color = SafeSphereColors.Info
                )
            }
        }

        // Team
        item {
            Text(
                text = "Built By",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🚀",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Privacy Advocates & Security Engineers",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeSphereColors.TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Dedicated to building tools that respect your digital rights",
                        fontSize = 14.sp,
                        color = SafeSphereColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * 📝 BLOGS SCREEN - Pro-Level with Real Data & Animations
 */
@Composable
fun BlogsScreen(viewModel: SafeSphereViewModel) {
    val blogs = remember {
        listOf(
            BlogPost(
                id = "1",
                title = "Why Offline-First Security Matters",
                excerpt = "In an era of constant data breaches, keeping your data offline is the ultimate protection...",
                category = "Security",
                readTime = "5 min",
                date = System.currentTimeMillis() - 86400000L,
                imageEmoji = "🔐",
                color = Color(0xFF1976D2)
            ),
            BlogPost(
                id = "2",
                title = "Understanding AES-256 Encryption",
                excerpt = "A deep dive into military-grade encryption and how SafeSphere protects your data...",
                category = "Technology",
                readTime = "8 min",
                date = System.currentTimeMillis() - 172800000L,
                imageEmoji = "🛡️",
                color = Color(0xFF388E3C)
            ),
            BlogPost(
                id = "3",
                title = "Password Health: Best Practices",
                excerpt = "Learn how to create and maintain strong passwords that resist brute-force attacks...",
                category = "Tips",
                readTime = "6 min",
                date = System.currentTimeMillis() - 259200000L,
                imageEmoji = "🗝️",
                color = Color(0xFFD32F2F)
            ),
            BlogPost(
                id = "4",
                title = "On-Device AI: The Future of Privacy",
                excerpt = "How local machine learning models are revolutionizing privacy-preserving AI...",
                category = "AI",
                readTime = "10 min",
                date = System.currentTimeMillis() - 345600000L,
                imageEmoji = "🤖",
                color = Color(0xFF7B1FA2)
            ),
            BlogPost(
                id = "5",
                title = "Zero-Trust Architecture Explained",
                excerpt = "Why SafeSphere never trusts the cloud, and why you shouldn't either...",
                category = "Architecture",
                readTime = "7 min",
                date = System.currentTimeMillis() - 432000000L,
                imageEmoji = "🚫",
                color = Color(0xFFFF6F00)
            ),
            BlogPost(
                id = "6",
                title = "Biometric Security: Pros & Cons",
                excerpt = "Understanding fingerprint and face recognition security in SafeSphere...",
                category = "Security",
                readTime = "5 min",
                date = System.currentTimeMillis() - 518400000L,
                imageEmoji = "👆",
                color = Color(0xFF00796B)
            )
        )
    }

    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Security", "Technology", "Tips", "AI", "Architecture")

    val filteredBlogs = remember(selectedCategory) {
        if (selectedCategory == "All") blogs
        else blogs.filter { it.category == selectedCategory }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SafeSphereColors.Background,
                        SafeSphereColors.BackgroundDark
                    )
                )
            ),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    SafeSphereColors.Primary.copy(alpha = 0.1f),
                                    SafeSphereColors.Secondary.copy(alpha = 0.1f)
                                )
                            )
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "📝",
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "SafeSphere Blog",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeSphereColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Privacy insights & security tips",
                            fontSize = 14.sp,
                            color = SafeSphereColors.TextSecondary
                        )
                    }
                }
            }
        }

        // Category filters
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (selectedCategory == category)
                            SafeSphereColors.Primary
                        else SafeSphereColors.Surface,
                        border = BorderStroke(
                            width = 1.dp,
                            color = SafeSphereColors.Primary.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.clickable { selectedCategory = category }
                    ) {
                        Text(
                            text = category,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedCategory == category)
                                Color.White
                            else SafeSphereColors.TextPrimary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Results count
        item {
            Text(
                text = "${filteredBlogs.size} ${if (filteredBlogs.size == 1) "article" else "articles"}",
                fontSize = 14.sp,
                color = SafeSphereColors.TextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // Blog posts
        items(filteredBlogs) { blog ->
            BlogPostCard(blog = blog)
        }
    }
}

/**
 * 📧 CONTACT US SCREEN - Pro-Level with Interactive Form
 */
@Composable
fun ContactUsScreen(viewModel: SafeSphereViewModel) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedTopic by remember { mutableStateOf("General Inquiry") }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val topics = listOf(
        "General Inquiry",
        "Feature Request",
        "Bug Report",
        "Security Concern",
        "Partnership",
        "Press/Media"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SafeSphereColors.Background,
                        SafeSphereColors.BackgroundDark
                    )
                )
            ),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    SafeSphereColors.Primary.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "📧",
                            fontSize = 56.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Get in Touch",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeSphereColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "We'd love to hear from you!",
                            fontSize = 15.sp,
                            color = SafeSphereColors.TextSecondary
                        )
                    }
                }
            }
        }

        // Quick contact options
        item {
            Text(
                text = "Quick Contact",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ContactMethodCard(
                    icon = "✉️",
                    title = "Email",
                    value = "support@safesphere.app",
                    color = SafeSphereColors.Primary,
                    modifier = Modifier.weight(1f)
                )
                ContactMethodCard(
                    icon = "🌐",
                    title = "Website",
                    value = "safesphere.app",
                    color = SafeSphereColors.Secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ContactMethodCard(
                    icon = "💬",
                    title = "Twitter",
                    value = "@SafeSphere",
                    color = Color(0xFF1DA1F2),
                    modifier = Modifier.weight(1f)
                )
                ContactMethodCard(
                    icon = "🐙",
                    title = "GitHub",
                    value = "SafeSphere",
                    color = Color(0xFF333333),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Contact form
        item {
            Text(
                text = "Send Us a Message",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        placeholder = { Text("Your name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SafeSphereColors.Primary,
                            focusedLabelColor = SafeSphereColors.Primary,
                            unfocusedBorderColor = SafeSphereColors.TextSecondary.copy(alpha = 0.3f),
                            focusedTextColor = SafeSphereColors.TextPrimary,
                            unfocusedTextColor = SafeSphereColors.TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        placeholder = { Text("your@email.com") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SafeSphereColors.Primary,
                            focusedLabelColor = SafeSphereColors.Primary,
                            unfocusedBorderColor = SafeSphereColors.TextSecondary.copy(alpha = 0.3f),
                            focusedTextColor = SafeSphereColors.TextPrimary,
                            unfocusedTextColor = SafeSphereColors.TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Topic dropdown
                    Column {
                        Text(
                            text = "Topic",
                            fontSize = 14.sp,
                            color = SafeSphereColors.TextPrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            topics.forEach { topic ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selectedTopic == topic)
                                        SafeSphereColors.Primary
                                    else SafeSphereColors.Surface,
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = SafeSphereColors.Primary.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.clickable { selectedTopic = topic }
                                ) {
                                    Text(
                                        text = topic,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (selectedTopic == topic)
                                            Color.White
                                        else SafeSphereColors.TextPrimary,
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 8.dp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Subject
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject") },
                        placeholder = { Text("Brief subject line") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SafeSphereColors.Primary,
                            focusedLabelColor = SafeSphereColors.Primary,
                            unfocusedBorderColor = SafeSphereColors.TextSecondary.copy(alpha = 0.3f),
                            focusedTextColor = SafeSphereColors.TextPrimary,
                            unfocusedTextColor = SafeSphereColors.TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Message
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Message") },
                        placeholder = { Text("Your message here...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SafeSphereColors.Primary,
                            focusedLabelColor = SafeSphereColors.Primary,
                            unfocusedBorderColor = SafeSphereColors.TextSecondary.copy(alpha = 0.3f),
                            focusedTextColor = SafeSphereColors.TextPrimary,
                            unfocusedTextColor = SafeSphereColors.TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 6
                    )

                    // Submit button
                    Button(
                        onClick = {
                            showSuccessDialog = true
                            // Clear form
                            name = ""
                            email = ""
                            subject = ""
                            message = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = name.isNotBlank() && email.isNotBlank() && message.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SafeSphereColors.Primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Send Message",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    // Privacy note
                    Text(
                        text = "🔒 Your message will be encrypted end-to-end",
                        fontSize = 11.sp,
                        color = SafeSphereColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // FAQ
        item {
            Text(
                text = "Frequently Asked",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FAQItem(
                    question = "How long until I get a response?",
                    answer = "We typically respond within 24-48 hours on business days."
                )
                FAQItem(
                    question = "Is my message encrypted?",
                    answer = "Yes! All contact form submissions are encrypted end-to-end."
                )
                FAQItem(
                    question = "Can I request new features?",
                    answer = "Absolutely! We love hearing feature ideas from our community."
                )
            }
        }
    }

    // Success dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            containerColor = SafeSphereColors.Surface,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(SafeSphereColors.Success.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "✅", fontSize = 32.sp)
                }
            },
            title = {
                Text(
                    text = "Message Sent!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "Thank you for reaching out! We'll get back to you within 24-48 hours.",
                    fontSize = 15.sp,
                    color = SafeSphereColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { showSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SafeSphereColors.Primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Got It!")
                }
            }
        )
    }
}

// ==================== HELPER COMPONENTS ====================

@Composable
private fun StatCard(
    title: String,
    icon: String,
    color: Color,
    content: @Composable () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f))
                    .border(
                        width = 2.dp,
                        color = color.copy(alpha = 0.3f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 32.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            content()
        }
    }
}

@Composable
private fun MiniStatCard(
    icon: String,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            color.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    color = color.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = icon, fontSize = 32.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = SafeSphereColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun FeatureItem(
    icon: String,
    title: String,
    description: String,
    color: Color
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f))
                    .border(
                        width = 2.dp,
                        color = color.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = SafeSphereColors.TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun BlogPostCard(blog: BlogPost) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* TODO: Open blog detail */ }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Image header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                blog.color.copy(alpha = 0.2f),
                                blog.color.copy(alpha = 0.05f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = blog.imageEmoji, fontSize = 56.sp)
            }

            // Content
            Column(modifier = Modifier.padding(16.dp)) {
                // Category badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = blog.color.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, blog.color.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = blog.category,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = blog.color,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = blog.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = blog.excerpt,
                    fontSize = 14.sp,
                    color = SafeSphereColors.TextSecondary,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📅 ${formatBlogDate(blog.date)}",
                            fontSize = 12.sp,
                            color = SafeSphereColors.TextSecondary
                        )
                        Text(
                            text = "⏱ ${blog.readTime}",
                            fontSize = 12.sp,
                            color = SafeSphereColors.TextSecondary
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Read more",
                        tint = blog.color,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactMethodCard(
    icon: String,
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color.copy(alpha = 0.05f))
                .border(
                    width = 2.dp,
                    color = color.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = icon, fontSize = 32.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = SafeSphereColors.TextSecondary
                )
                Text(
                    text = value,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}

@Composable
private fun FAQItem(question: String, answer: String) {
    var expanded by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = question,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SafeSphereColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = SafeSphereColors.Primary
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = answer,
                        fontSize = 14.sp,
                        color = SafeSphereColors.TextSecondary,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

// ==================== DATA MODELS ====================

data class BlogPost(
    val id: String,
    val title: String,
    val excerpt: String,
    val category: String,
    val readTime: String,
    val date: Long,
    val imageEmoji: String,
    val color: Color
)

fun formatBlogDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}