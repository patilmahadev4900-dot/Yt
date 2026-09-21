package com.example.autoshorts.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.autoshorts.engine.VoiceMasteringEngine
import com.example.autoshorts.model.KenBurnsDirection
import com.example.autoshorts.model.ShortsScript
import com.example.autoshorts.model.SupportedLanguage
import com.example.autoshorts.model.ToneMode
import com.example.ui.theme.HighlightYellow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.YoutubeRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ShortsPlayerView(
    script: ShortsScript,
    modifier: Modifier = Modifier,
    onSfxTriggered: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val voiceEngine = remember { VoiceMasteringEngine(context) }
    val scope = rememberCoroutineScope()

    var isPlaying by remember { mutableStateOf(false) }
    var currentWordIndex by remember { mutableIntStateOf(0) }
    var playbackProgress by remember { mutableFloatStateOf(0f) }
    var activeSfx by remember { mutableStateOf<String?>(null) }
    var currentDirection by remember { mutableStateOf(KenBurnsDirection.IN) }

    // Parse words from narration without [pause] tokens
    val words = remember(script.narration) {
        script.narration
            .replace("[pause]", " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceEngine.release()
        }
    }

    // Infinite transition for smooth Ken-Burns simulation
    val infiniteTransition = rememberInfiniteTransition(label = "ken_burns")
    val zoomAnim by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "zoom"
    )
    val panAnim by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pan"
    )

    // Playback loop controller
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val totalDurationMs = (script.durationSec * 1000).toLong()
            val totalWordsCount = words.size.coerceAtLeast(1)
            val msPerWord = totalDurationMs / totalWordsCount

            val toneMode = ToneMode.fromId(script.tone)
            val lang = SupportedLanguage.fromCode(script.language)

            // Start Speech synthesis with tone prosody
            voiceEngine.speakScript(
                narration = script.narration,
                tone = toneMode,
                language = lang
            ) { curr, total, _ ->
                currentWordIndex = curr
            }

            val startTime = System.currentTimeMillis()

            while (isPlaying && playbackProgress < 1.0f) {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / totalDurationMs).coerceIn(0f, 1f)
                playbackProgress = progress

                val calculatedWordIndex = (progress * totalWordsCount).toInt().coerceIn(0, totalWordsCount - 1)
                if (calculatedWordIndex != currentWordIndex) {
                    currentWordIndex = calculatedWordIndex

                    // Check for SFX beat match
                    val currentTimeSec = progress * script.durationSec
                    val matchedBeat = script.visualBeats.firstOrNull { beat ->
                        kotlin.math.abs(beat.t - currentTimeSec) < 0.6f
                    }
                    if (matchedBeat != null && activeSfx != matchedBeat.sfx) {
                        activeSfx = matchedBeat.sfx
                        voiceEngine.playSfx(matchedBeat.sfx)
                        onSfxTriggered?.invoke(matchedBeat.sfx)
                    } else if (matchedBeat == null) {
                        activeSfx = null
                    }
                }

                delay(50)
            }

            if (playbackProgress >= 1.0f) {
                isPlaying = false
                playbackProgress = 0f
                currentWordIndex = 0
                activeSfx = null
            }
        } else {
            voiceEngine.stop()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black)
            .border(1.5.dp, Color(0xFF3B2F57), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        // 1. Ken-Burns Animated Visual Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoomAnim
                    scaleY = zoomAnim
                    translationX = panAnim
                }
        ) {
            VisualBackdropCanvas(
                tone = script.tone,
                progress = playbackProgress
            )
        }

        // 2. Cinematic Vignette Overlay (Enhanced Contrast & Saturation feel)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x99000000),
                            Color(0x22000000),
                            Color(0x33000000),
                            Color(0xDD000000)
                        )
                    )
                )
        )

        // 3. Top Header: Title & Tone Badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xAA1E1233))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(YoutubeRed)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = script.tone.uppercase(),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xAA1E1233))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Audio Target",
                    tint = NeonCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "-14 LUFS",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // 4. SFX Trigger Alert Banner
        AnimatedVisibility(
            visible = activeSfx != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp)
        ) {
            Surface(
                color = YoutubeRed,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 6.dp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "⚡ SFX: ${activeSfx?.uppercase()}",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // 5. Karaoke Subtitles (Positioned strictly around 68% height per Python config)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .offset(y = (heightPx * 0.58f).dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            KaraokeSubtitlesText(
                words = words,
                activeIndex = currentWordIndex
            )
        }

        // 6. Bottom Controls: Progress & Playback
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(14.dp)
        ) {
            // Title & Hook Preview
            Text(
                text = script.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Text(
                text = script.hook,
                color = HighlightYellow,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { playbackProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = YoutubeRed,
                trackColor = Color(0x44FFFFFF),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${(playbackProgress * script.durationSec).toInt()}s / ${script.durationSec}s",
                    color = Color(0xFFD1D5DB),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            playbackProgress = 0f
                            currentWordIndex = 0
                            isPlaying = true
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("player_replay_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = "Replay",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(YoutubeRed)
                            .testTag("player_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KaraokeSubtitlesText(
    words: List<String>,
    activeIndex: Int
) {
    if (words.isEmpty()) return

    // Context window: max 3 words before + active word + max 4 words after
    val start = (activeIndex - 3).coerceAtLeast(0)
    val end = (activeIndex + 5).coerceAtMost(words.size)
    val activeOffset = activeIndex - start

    val windowWords = words.subList(start, end)

    val annotated = buildAnnotatedString {
        windowWords.forEachIndexed { idx, word ->
            if (idx == activeOffset) {
                // Highlighted word: Bold, Yellow (#FDE047), larger scale
                withStyle(
                    SpanStyle(
                        color = HighlightYellow,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        shadow = Shadow(
                            color = Color.Black,
                            offset = Offset(2f, 2f),
                            blurRadius = 6f
                        )
                    )
                ) {
                    append(word.uppercase())
                }
            } else {
                withStyle(
                    SpanStyle(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        shadow = Shadow(
                            color = Color.Black,
                            offset = Offset(1.5f, 1.5f),
                            blurRadius = 4f
                        )
                    )
                ) {
                    append(word)
                }
            }
            if (idx < windowWords.size - 1) append(" ")
        }
    }

    Surface(
        color = Color(0x99000000),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Text(
            text = annotated,
            textAlign = TextAlign.Center,
            lineHeight = 26.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun VisualBackdropCanvas(
    tone: String,
    progress: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        when (tone.lowercase()) {
            "kids" -> {
                // Cheerful vibrant cosmic gradients with glowing stars
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF6B21A8), Color(0xFF1E1B4B), Color(0xFF0F0C20)),
                        center = Offset(w * 0.5f, h * 0.4f),
                        radius = w * 0.8f
                    )
                )
                // Sparkle circles
                val starCount = 18
                for (i in 0 until starCount) {
                    val sx = (w * ((i * 37) % 100) / 100f)
                    val sy = (h * ((i * 53) % 100) / 100f)
                    val radius = 3f + (i % 4) * 2f
                    drawCircle(
                        color = Color(0xFFFDE047).copy(alpha = 0.6f + (i % 3) * 0.15f),
                        radius = radius,
                        center = Offset(sx, sy)
                    )
                }
            }
            "business" -> {
                // Deep navy with golden compounding graph lines & sleek bars
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF0A192F), Color(0xFF172A45), Color(0xFF060D17)),
                        start = Offset(0f, 0f),
                        end = Offset(w, h)
                    )
                )
                // Geometric grid accents
                for (step in 1..6) {
                    val y = h * (step / 8f)
                    drawLine(
                        color = Color(0x1A64FFDA),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.5f
                    )
                }
            }
            "story" -> {
                // Atmospheric cinematic shadows with warm amber candlelight
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF78350F), Color(0xFF291507), Color(0xFF0A0502)),
                        center = Offset(w * 0.3f, h * 0.6f),
                        radius = w * 0.9f
                    )
                )
            }
            else -> {
                // Tech: Cyber obsidian with cyan neon grid lines
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF0F172A), Color(0xFF1E1035), Color(0xFF030712)),
                        start = Offset(0f, 0f),
                        end = Offset(w, h)
                    )
                )
                // Circuit grid lines
                for (step in 1..8) {
                    val y = h * (step / 9f)
                    drawLine(
                        color = Color(0x1F38BDF8),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.5f
                    )
                }
            }
        }
    }
}
