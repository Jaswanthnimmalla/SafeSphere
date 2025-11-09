package com.runanywhere.startup_hackathon20.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runanywhere.startup_hackathon20.sync.DesktopSyncServer
import com.runanywhere.startup_hackathon20.viewmodels.SafeSphereViewModel
import com.runanywhere.startup_hackathon20.ui.SafeSphereColors
import com.runanywhere.startup_hackathon20.ui.SafeSphereHeader
import com.runanywhere.startup_hackathon20.ui.GlassCard

/**
 * Desktop Sync Screen - Cross-Platform Password Sync
 *
 * This screen allows users to sync passwords with desktop via local network
 */
@Composable
fun DesktopSyncScreen(viewModel: SafeSphereViewModel) {
    val context = LocalContext.current

    // Use a singleton server that persists across navigation
    val syncServer = remember {
        DesktopSyncServerSingleton.getInstance(context)
    }

    val isRunning by syncServer.isRunning.collectAsState()
    val connectedClients by syncServer.connectedClients.collectAsState()

    // DON'T cleanup on dispose - let server run persistently
    // Only stop when user explicitly taps "Stop" button

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        SafeSphereHeader(
            title = "Desktop Sync",
            subtitle = "Cross-Platform Access",
            onBackClick = { viewModel.navigateBack() }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Main Feature Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🖥️",
                    fontSize = 64.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Desktop Companion",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Access SafeSphere from any browser",
                    fontSize = 14.sp,
                    color = SafeSphereColors.TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Status Card with Clickable Link
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isRunning)
                    Color(0xFFD4EDDA)
                else
                Color(0xFFF8D7DA)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isRunning) "✅ Server Running" else "⭕ Server Stopped",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isRunning) Color(0xFF155724) else Color(0xFF721C24)
                )

                if (isRunning) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "$connectedClients connected device(s)",
                        fontSize = 14.sp,
                        color = Color(0xFF155724)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Clickable URL
                    val url = syncServer.getConnectionUrl()
                    Text(
                        text = "Tap to open in browser:",
                        fontSize = 12.sp,
                        color = Color(0xFF155724).copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = url,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0066CC),
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .clickable {
                                // Open URL in browser
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            }
                            .padding(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "👆 Click above to launch SafeSphere Desktop",
                        fontSize = 11.sp,
                        color = Color(0xFF155724).copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Security warning note
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF3CD)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "ℹ️", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Browser Security Note",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF856404)
                                )
                                Text(
                                    text = "If your browser shows a security warning, click 'Continue to site' or 'Advanced' → 'Proceed'. This is safe - the server runs on your local network only!",
                                    fontSize = 11.sp,
                                    color = Color(0xFF856404),
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Start/Stop Button
        Button(
            onClick = {
                if (isRunning) {
                    syncServer.stopServer()
                } else {
                    syncServer.startServer()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) Color(0xFFDC3545) else SafeSphereColors.Primary
            )
        ) {
            Text(
                text = if (isRunning) "🛑 Stop Desktop Sync" else "🚀 Start Desktop Sync",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Persistent Mode Notice
        if (isRunning) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3CD)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🔄", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Persistent Mode Active",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF856404)
                        )
                        Text(
                            text = "Server will keep running even when you navigate away. Use Stop button to turn off.",
                            fontSize = 12.sp,
                            color = Color(0xFF856404)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Instructions Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "📖 How It Works",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                InstructionStep(
                    number = "1",
                    text = "Tap 'Start Desktop Sync' button above"
                )

                InstructionStep(
                    number = "2",
                    text = "Tap the blue link to open SafeSphere in your browser"
                )

                InstructionStep(
                    number = "3",
                    text = "Make sure phone and computer are on the same WiFi"
                )

                InstructionStep(
                    number = "4",
                    text = "View all SafeSphere features on the big screen!"
                )

                InstructionStep(
                    number = "5",
                    text = "Server keeps running even when you close this screen"
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = SafeSphereColors.Primary.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔒",
                            fontSize = 24.sp
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "100% Secure & Offline",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SafeSphereColors.Primary
                            )

                            Text(
                                text = "All data stays on your local network. No cloud. No external servers!",
                                fontSize = 12.sp,
                                color = SafeSphereColors.TextSecondary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Feature Status Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ℹ️",
                        fontSize = 24.sp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Feature Status",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeSphereColors.TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "This feature is currently in development. The full web interface with real-time sync will be available after running:",
                    fontSize = 14.sp,
                    color = SafeSphereColors.TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF0F4F8)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "./gradlew build",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeSphereColors.Primary,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "This will download NanoHTTPD dependencies and enable the full desktop web app with beautiful UI and WebSocket sync.",
                    fontSize = 13.sp,
                    color = SafeSphereColors.TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Benefits Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "✨ Features on Desktop",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                BenefitItem("🔑", "Password Manager", "View all features")
                BenefitItem("🤖", "AI Security Predictor", "ML predictions")
                BenefitItem("🎤", "Voice Control", "8 languages")
                BenefitItem("🏥", "Password Health", "Real-time analysis")
                BenefitItem("🖥️", "All 16 Features", "Complete access")
            }
        }
    }
}

/**
 * Singleton to keep server running across navigation
 */
object DesktopSyncServerSingleton {
    private var instance: DesktopSyncServer? = null

    fun getInstance(context: Context): DesktopSyncServer {
        if (instance == null) {
            instance = DesktopSyncServer(context.applicationContext)
        }
        return instance!!
    }
}

@Composable
private fun InstructionStep(number: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    SafeSphereColors.Primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.Primary
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            fontSize = 14.sp,
            color = SafeSphereColors.TextSecondary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BenefitItem(icon: String, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = icon,
            fontSize = 24.sp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = SafeSphereColors.TextPrimary
            )

            Text(
                text = description,
                fontSize = 13.sp,
                color = SafeSphereColors.TextSecondary
            )
        }
    }
}
