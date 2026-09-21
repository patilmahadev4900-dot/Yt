package com.example.autoshorts.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.autoshorts.engine.PipelineOrchestrator
import com.example.autoshorts.model.SupportedLanguage
import com.example.autoshorts.model.ToneMode
import com.example.ui.theme.HighlightYellow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioDarkSurface
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.YoutubeRed

@Composable
fun SettingsScreen(
    orchestrator: PipelineOrchestrator,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("autoshorts_prefs", Context.MODE_PRIVATE) }
    var userApiKey by remember {
        mutableStateOf(
            sharedPrefs.getString("custom_gemini_api_key", "") ?: ""
        )
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBlack)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = HighlightYellow,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Pipeline Configuration",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Broadcast-grade standards & API keys",
                    color = StudioTextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // API Key Section
        SettingsCard(
            title = "GEMINI API KEY",
            icon = Icons.Default.Key,
            iconTint = HighlightYellow
        ) {
            Text(
                text = "Used by the Script Engine for curiosity hooks, banned-word gates, and SEO metadata.",
                color = StudioTextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = userApiKey,
                onValueChange = { userApiKey = it },
                placeholder = {
                    Text(
                        text = if (BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY")
                            "Configured via AI Studio Secrets (Active)"
                        else
                            "Paste Gemini API Key (e.g. AIzaSy...)",
                        fontSize = 12.sp,
                        color = StudioTextSecondary
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("api_key_text_field"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = YoutubeRed,
                    unfocusedBorderColor = StudioCardBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    sharedPrefs.edit().putString("custom_gemini_api_key", userApiKey.trim()).apply()
                    Toast.makeText(context, "API Key saved successfully!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = YoutubeRed),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .align(Alignment.End)
                    .testTag("save_api_key_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Key", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Audio Mastering Standards
        SettingsCard(
            title = "AUDIO MASTERING (EBU R128 / YOUTUBE)",
            icon = Icons.Default.AudioFile,
            iconTint = NeonCyan
        ) {
            StandardRow("Target Loudness", "-14.0 LUFS", "Strict YouTube Shorts standard")
            StandardRow("True Peak Limit", "-1.5 dB", "Inter-sample clipping prevention")
            StandardRow("Loudness Range (LRA)", "11.0", "Dynamic broadcast range")
            StandardRow("High-Pass Filter", "80 Hz", "Sub-bass rumble removal")
            StandardRow("Voice Ducking", "-22.0 dB", "Music ducked under narration")
            StandardRow("Music Ambient Rise", "-14.0 dB", "Music volume during micro-pauses")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Video & Motion Standards
        SettingsCard(
            title = "VIDEO & TIMELINE ENGINE",
            icon = Icons.Default.Videocam,
            iconTint = YoutubeRed
        ) {
            StandardRow("Aspect Ratio", "9:16 Vertical", "1080 x 1920 pixels")
            StandardRow("Frame Rate", "30 FPS", "Standard mobile streaming")
            StandardRow("Max Clip Duration", "2.5 sec", "Aggressive visual pacing")
            StandardRow("Contrast Boost", "+10% (1.10x)", "Punchy mobile screen pop")
            StandardRow("Saturation Boost", "+8% (1.08x)", "Vibrant color grading")
            StandardRow("Ken-Burns Motion", "In / Out / Left / Right", "Continuous dynamic pan & zoom")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Subtitle & Karaoke Standards
        SettingsCard(
            title = "KARAOKE SUBTITLES SPECIFICATION",
            icon = Icons.Default.Subtitles,
            iconTint = HighlightYellow
        ) {
            StandardRow("Vertical Anchor", "68% Height", "Optimal eye-tracking center")
            StandardRow("Active Highlight", "#FDE047 (Electric Gold)", "Real-time word highlight")
            StandardRow("Stroke / Outline", "Black (4px)", "High-contrast legibility")
            StandardRow("Context Window", "3 Before + 4 After", "Fluid peripheral reading")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scheduled Times
        SettingsCard(
            title = "AUTONOMOUS UPLOAD SCHEDULE",
            icon = Icons.Default.Schedule,
            iconTint = NeonEmerald
        ) {
            Text(
                text = "Cron triggers set to run pipeline and queue upload batches:",
                color = StudioTextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            listOf("06:00 UTC (Morning Rush)", "12:00 UTC (Midday Peak)", "16:00 UTC (Afternoon Buzz)", "20:00 UTC (Evening Prime)").forEach { slot ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = slot, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text(text = "ACTIVE", color = NeonEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, StudioCardBorder, RoundedCornerShape(16.dp)),
        color = StudioCardSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun StandardRow(label: String, value: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = StudioTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(text = value, color = HighlightYellow, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        Text(text = subtitle, color = StudioTextSecondary, fontSize = 10.sp)
    }
}
