package com.example.autoshorts.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VideoSettings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.autoshorts.engine.PipelineOrchestrator
import com.example.autoshorts.model.PipelineStatus
import com.example.autoshorts.model.SupportedLanguage
import com.example.autoshorts.model.ToneMode
import com.example.autoshorts.model.TopicBank
import com.example.autoshorts.ui.components.PipelineLogSheet
import com.example.autoshorts.ui.components.ShortsPlayerView
import com.example.autoshorts.ui.components.YouTubeMetadataCard
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudioScreen(
    orchestrator: PipelineOrchestrator,
    modifier: Modifier = Modifier
) {
    val status by orchestrator.status.collectAsState()
    val script by orchestrator.currentScript.collectAsState()
    val logs by orchestrator.logs.collectAsState()
    val progress by orchestrator.progressPercent.collectAsState()
    val isAutoScheduleEnabled by orchestrator.isAutoScheduleEnabled.collectAsState()

    var selectedTone by remember { mutableStateOf(ToneMode.TECH) }
    var selectedLanguage by remember { mutableStateOf(SupportedLanguage.EN) }
    var topicInput by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBlack)
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Bar & Branding
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(YoutubeRed),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "▶",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "AutoShorts",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Ultra-High Production Pipeline",
                        color = StudioTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // Live status badge
            Surface(
                color = when (status) {
                    PipelineStatus.COMPLETED -> NeonEmerald.copy(alpha = 0.15f)
                    PipelineStatus.ERROR -> YoutubeRed.copy(alpha = 0.15f)
                    PipelineStatus.IDLE -> StudioDarkSurface
                    else -> NeonCyan.copy(alpha = 0.15f)
                },
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    when (status) {
                        PipelineStatus.COMPLETED -> NeonEmerald
                        PipelineStatus.ERROR -> YoutubeRed
                        PipelineStatus.IDLE -> StudioCardBorder
                        else -> NeonCyan
                    }
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                when (status) {
                                    PipelineStatus.COMPLETED -> NeonEmerald
                                    PipelineStatus.ERROR -> YoutubeRed
                                    PipelineStatus.IDLE -> Color(0xFF64748B)
                                    else -> NeonCyan
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (status) {
                            PipelineStatus.IDLE -> "IDLE"
                            PipelineStatus.PICKING_TOPIC -> "PICKING"
                            PipelineStatus.GENERATING_SCRIPT -> "AI SCRIPT"
                            PipelineStatus.SYNTHESIZING_VOICE -> "VOICE PROSODY"
                            PipelineStatus.MASTERING_AUDIO -> "-14 LUFS DSP"
                            PipelineStatus.ASSEMBLING_TIMELINE -> "KEN-BURNS"
                            PipelineStatus.COMPLETED -> "READY"
                            PipelineStatus.ERROR -> "ERROR"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Autonomous Scheduler Strip
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = StudioDarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = HighlightYellow,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Daily Automated Uploads",
                            color = StudioTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "06:00, 12:00, 16:00, 20:00 UTC",
                            color = StudioTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                Switch(
                    checked = isAutoScheduleEnabled,
                    onCheckedChange = { orchestrator.toggleAutoSchedule() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = YoutubeRed,
                        uncheckedTrackColor = StudioCardBorder
                    ),
                    modifier = Modifier.testTag("scheduler_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live 9:16 Shorts Player Preview
        val activeScript = script ?: TopicBank.getRandomTopic("tech").let { (t, tone) ->
            com.example.autoshorts.data.remote.GeminiScriptEngine().generateFallback(t, tone.id, "en")
        }

        ShortsPlayerView(
            script = activeScript,
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .testTag("shorts_player_view")
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Control Panel: Topic, Tone, Language
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = StudioCardSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PRODUCTION CONTROLS",
                        color = StudioTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    IconButton(
                        onClick = {
                            val (randTopic, randTone) = TopicBank.getRandomTopic()
                            topicInput = randTopic
                            selectedTone = randTone
                        },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("random_topic_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = "Pick Random Topic",
                            tint = HighlightYellow,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Custom Topic Input
                OutlinedTextField(
                    value = topicInput,
                    onValueChange = { topicInput = it },
                    placeholder = { Text("Topic (e.g., 'Why phone battery dies fast' or blank for random)", fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("topic_text_field"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = YoutubeRed,
                        unfocusedBorderColor = StudioCardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Tone Mode Selectors
                Text(
                    text = "TONAL MODE (Prosody & Energy)",
                    color = StudioTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ToneMode.entries.forEach { mode ->
                        val isSelected = selectedTone == mode
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedTone = mode }
                                .border(
                                    1.dp,
                                    if (isSelected) YoutubeRed else StudioCardBorder,
                                    RoundedCornerShape(10.dp)
                                ),
                            color = if (isSelected) YoutubeRed.copy(alpha = 0.2f) else StudioDarkSurface
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = mode.label,
                                    color = if (isSelected) HighlightYellow else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${mode.rate}x",
                                    color = StudioTextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Language Selectors
                Text(
                    text = "TARGET LANGUAGE",
                    color = StudioTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        SupportedLanguage.EN,
                        SupportedLanguage.HI,
                        SupportedLanguage.MR,
                        SupportedLanguage.ES,
                        SupportedLanguage.JA,
                        SupportedLanguage.FR
                    ).forEach { lang ->
                        val isSel = selectedLanguage == lang
                        Surface(
                            color = if (isSel) NeonCyan.copy(alpha = 0.2f) else StudioDarkSurface,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSel) NeonCyan else StudioCardBorder
                            ),
                            modifier = Modifier.clickable { selectedLanguage = lang }
                        ) {
                            Text(
                                text = lang.displayName,
                                color = if (isSel) NeonCyan else StudioTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Pipeline Progress Bar when running
                if (status != PipelineStatus.IDLE && status != PipelineStatus.COMPLETED && status != PipelineStatus.ERROR) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Running Autonomous Engine...",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                color = HighlightYellow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = YoutubeRed,
                            trackColor = StudioDarkSurface
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                // Run Autonomous Pipeline Button
                val isRunning = status != PipelineStatus.IDLE && status != PipelineStatus.COMPLETED && status != PipelineStatus.ERROR
                Button(
                    onClick = {
                        orchestrator.runPipeline(
                            topicOverride = topicInput.ifBlank { null },
                            toneOverride = selectedTone,
                            languageOverride = selectedLanguage
                        )
                    },
                    enabled = !isRunning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = YoutubeRed,
                        disabledContainerColor = YoutubeRed.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("run_pipeline_button")
                ) {
                    if (isRunning) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "AUTONOMOUS AGENT ACTIVE...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = HighlightYellow,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "⚡ GENERATE & MASTER SHORT",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Terminal Pipeline Logs
        PipelineLogSheet(
            logs = logs,
            onClearLogs = { orchestrator.clearLogs() },
            modifier = Modifier.testTag("pipeline_logs_sheet")
        )

        Spacer(modifier = Modifier.height(18.dp))

        // YouTube Metadata Card
        if (script != null) {
            YouTubeMetadataCard(
                script = script!!,
                modifier = Modifier.testTag("youtube_metadata_card")
            )
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}
