// Security Score Card - CENTERED VERSION
// Copy this entire section to replace the existing Security Score Card in SafeSphereMainActivity.kt
// Location: Around line 1215-1260

GlassCard(
modifier = Modifier.fillMaxWidth()
) {
    Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Security Score",
            fontSize = 16.sp,
            color = SafeSphereColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ✅ FIXED: Added .fillMaxWidth() to center the circle
        Box(
            modifier = Modifier.fillMaxWidth(),  // ← THIS IS THE FIX
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = stats.securityScore / 100f,
                modifier = Modifier.size(120.dp),
                strokeWidth = 12.dp,
                color = when {
                    stats.securityScore >= 90 -> SafeSphereColors.Success
                    stats.securityScore >= 70 -> SafeSphereColors.Warning
                    else -> SafeSphereColors.Error
                },
                trackColor = SafeSphereColors.TextSecondary.copy(alpha = 0.1f)
            )

            Text(
                text = "${stats.securityScore}",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "${stats.encryptedItems} of ${stats.totalItems} items encrypted",
            fontSize = 14.sp,
            color = SafeSphereColors.TextSecondary
        )
    }
}

/* 
INSTRUCTIONS:
1. Open SafeSphereMainActivity.kt
2. Find "// Security Score Card" (around line 1215)
3. Select the entire GlassCard block for Security Score
4. Replace it with the code above
5. Build: ./gradlew assembleDebug
6. The circle will now be centered!
*/