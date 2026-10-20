package com.auravoice.tts.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auravoice.tts.engine.TtsGenerationState
import com.auravoice.tts.util.AudioExportUtils
import com.auravoice.tts.ui.components.AudioWaveformVisualizer
import com.auravoice.tts.ui.components.PremiumVoicePromotionDialog

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState

val CrownIcon: ImageVector
    get() {
        if (_crownIcon != null) return _crownIcon!!
        _crownIcon = ImageVector.Builder(
            name = "Crown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color(0xFFFFB800))) {
                moveTo(3f, 16f)
                lineTo(21f, 16f)
                lineTo(21f, 18f)
                lineTo(3f, 18f)
                close()
                moveTo(5f, 15f)
                lineTo(2f, 7f)
                lineTo(7f, 10f)
                lineTo(12f, 4f)
                lineTo(17f, 10f)
                lineTo(22f, 7f)
                lineTo(19f, 15f)
                close()
            }
        }.build()
        return _crownIcon!!
    }
private var _crownIcon: ImageVector? = null

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToVoices: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToSubscription: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val availableVoices by viewModel.availableVoices.collectAsStateWithLifecycle()
    val availableLanguages by viewModel.availableLanguages.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()
    val selectedVoice = uiState.selectedVoice
    val context = LocalContext.current

    var showLanguageDropdown by remember { mutableStateOf(false) }
    var showVoiceDropdown by remember { mutableStateOf(false) }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = { viewModel.refreshData() },
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
                .verticalScroll(scrollState)
                .testTag("home_screen")
        ) {
        // Modern Clean Top Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Title & Brand
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "AuraVoice",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                if (userProfile.isPremium) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFFB800))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PRO",
                            color = Color.Black,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Quick Action Header Icons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Subscription Crown Icon
                IconButton(
                    onClick = onNavigateToSubscription,
                    modifier = Modifier.testTag("btn_top_premium")
                ) {
                    Icon(
                        imageVector = CrownIcon,
                        contentDescription = "Go Premium",
                        tint = Color(0xFFFFB800),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Profile Avatar Icon
                IconButton(
                    onClick = onNavigateToProfile,
                    modifier = Modifier.testTag("btn_top_profile")
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "User Profile",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Studio Text Generation Canvas
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("canvas_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Top Row Inside Canvas: Language & Voice Selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Language Dropdown Chip
                        Box {
                            val flagEmoji = com.auravoice.tts.ui.screens.voices.getFlagEmoji(uiState.selectedLanguage)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                                    .clickable { showLanguageDropdown = true }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                                    .testTag("selected_language_chip")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = flagEmoji, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = uiState.selectedLanguage,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Language",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showLanguageDropdown,
                                onDismissRequest = { showLanguageDropdown = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                availableLanguages.forEach { lang ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(com.auravoice.tts.ui.screens.voices.getFlagEmoji(lang), fontSize = 14.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = lang,
                                                    fontWeight = if (lang.equals(uiState.selectedLanguage, ignoreCase = true)) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (lang.equals(uiState.selectedLanguage, ignoreCase = true)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.onSelectLanguage(lang)
                                            showLanguageDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // 2. Voice Dropdown Chip
                        Box {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (selectedVoice != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, if (selectedVoice != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                                    .clickable { showVoiceDropdown = true }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                                    .testTag("selected_voice_chip")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(if (selectedVoice != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = selectedVoice?.name ?: "Select Voice",
                                        color = if (selectedVoice != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Voice",
                                        tint = if (selectedVoice != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    if (selectedVoice != null) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        val isPlayingSelectedSample = playbackState.isPlaying && playbackState.currentAudioPathOrUrl == selectedVoice.voiceSampleUrl
                                        val isBufferingSelectedSample = playbackState.isBuffering && playbackState.currentAudioPathOrUrl == selectedVoice.voiceSampleUrl
                                        if (isBufferingSelectedSample) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = MaterialTheme.colorScheme.primary,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (isPlayingSelectedSample) Icons.Default.Pause else Icons.Default.VolumeUp,
                                                contentDescription = "Preview Voice Sample",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable { viewModel.previewVoiceSample(selectedVoice) }
                                            )
                                        }
                                    }
                                }
                            }

                            val voicesInLang = availableVoices.filter {
                                it.language.equals(uiState.selectedLanguage, ignoreCase = true) ||
                                it.language.contains(uiState.selectedLanguage, ignoreCase = true) ||
                                uiState.selectedLanguage.contains(it.language, ignoreCase = true)
                            }.ifEmpty { availableVoices }

                            DropdownMenu(
                                expanded = showVoiceDropdown,
                                onDismissRequest = { showVoiceDropdown = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                voicesInLang.forEach { voice ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = voice.name,
                                                            fontWeight = if (voice.id == selectedVoice?.id) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (voice.id == selectedVoice?.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                        )
                                                        if (voice.isPremium) {
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = "PRO",
                                                                color = MaterialTheme.colorScheme.secondary,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier
                                                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = "${voice.gender} • ${voice.accent}",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    val isPlayingSample = playbackState.isPlaying && playbackState.currentAudioPathOrUrl == voice.voiceSampleUrl
                                                    val isBufferingSample = playbackState.isBuffering && playbackState.currentAudioPathOrUrl == voice.voiceSampleUrl
                                                    IconButton(
                                                        onClick = { viewModel.previewVoiceSample(voice) },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        if (isBufferingSample) {
                                                            CircularProgressIndicator(
                                                                modifier = Modifier.size(16.dp),
                                                                color = MaterialTheme.colorScheme.primary,
                                                                strokeWidth = 2.dp
                                                            )
                                                        } else {
                                                            Icon(
                                                                imageVector = if (isPlayingSample) Icons.Default.Pause else Icons.Default.VolumeUp,
                                                                contentDescription = "Preview Sample Voice",
                                                                tint = if (isPlayingSample) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        onClick = {
                                            viewModel.onSelectVoice(voice)
                                            showVoiceDropdown = false
                                        }
                                    )
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "🌐 Browse Voice Catalog...",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    },
                                    onClick = {
                                        showVoiceDropdown = false
                                        onNavigateToVoices()
                                    }
                                )
                            }
                        }
                    }

                    if (uiState.inputText.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.onInputTextChange("") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Clear Prompt",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Text Canvas Input Area
                OutlinedTextField(
                    value = uiState.inputText,
                    onValueChange = { viewModel.onInputTextChange(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .testTag("input_text_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    placeholder = {
                        Text(
                            text = "Create natural-sounding voiceovers with AuraVoice...",
                            color = MaterialTheme.colorScheme.tertiary,
                            fontSize = 17.sp,
                            lineHeight = 24.sp
                        )
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Character counter & Sample text prompt
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.inputText.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.onInputTextChange("") }
                        ) {
                            Text("Clear Text", color = MaterialTheme.colorScheme.tertiary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        TextButton(
                            onClick = {
                                viewModel.onInputTextChange("AuraVoice delivers crystal-clear speech generation with realistic voice samples and custom voice library support.")
                            }
                        ) {
                            Text("Paste Sample Text", color = MaterialTheme.colorScheme.secondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    val charLength = uiState.inputText.length
                    val maxLimit = uiState.maxCharLimitPerRequest
                    val isOverLimit = charLength > maxLimit

                    Text(
                        text = "$charLength/$maxLimit",
                        color = if (isOverLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                        fontSize = 11.sp,
                        fontWeight = if (isOverLimit) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.testTag("text_character_counter")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Controls Bar at bottom of Canvas
                val isGenerating = uiState.generationState is TtsGenerationState.Processing
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left pill: "+ Voice"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                            .clickable { onNavigateToVoices() }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Voices",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Voices", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Generate Button
                        Button(
                            onClick = { viewModel.generateSpeechOnDevice() },
                            enabled = !isGenerating,
                            modifier = Modifier
                                .height(44.dp)
                                .testTag("btn_generate_speech"),
                            shape = RoundedCornerShape(22.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Generating...", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Text("Generate", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Error Banner
        if (uiState.errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = uiState.errorMessage!!,
                color = Color(0xFFEF4444),
                fontSize = 12.sp
            )
        }

        // Generation Progress Indicator
        if (uiState.generationState is TtsGenerationState.Processing) {
            val state = uiState.generationState as TtsGenerationState.Processing
            val labelText = state.currentWord
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(labelText, color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("${(state.progress * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Generated Audio Player Output Card
        AnimatedVisibility(visible = uiState.activeGeneratedAudioPath != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("audio_player_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Audio Output",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generated Speech", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        if (uiState.generationState is TtsGenerationState.Success) {
                            val success = uiState.generationState as TtsGenerationState.Success
                            Text("${success.generationTimeMs}ms", color = MaterialTheme.colorScheme.tertiary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    AudioWaveformVisualizer(
                        isPlaying = playbackState.isPlaying,
                        isGenerating = uiState.generationState is TtsGenerationState.Processing
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.playPauseGeneratedAudio() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("btn_play_pause_audio"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (playbackState.isPlaying) "Pause Audio" else "Play Speech",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                uiState.activeGeneratedAudioPath?.let { path ->
                                    val voiceName = selectedVoice?.name ?: "AuraVoice"
                                    AudioExportUtils.saveAudioToPublicAuraVoiceFolder(
                                        context = context,
                                        sourceFilePath = path,
                                        text = uiState.inputText,
                                        voiceName = voiceName
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("btn_save_audio"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Save Audio",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save File", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = {
                                uiState.activeGeneratedAudioPath?.let { path ->
                                    val voiceName = selectedVoice?.name ?: "AuraVoice"
                                    AudioExportUtils.shareAudioFile(
                                        context = context,
                                        filePath = path,
                                        text = uiState.inputText,
                                        voiceName = voiceName
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("btn_share_audio"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Audio",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

    // Premium Voice Promotion Dialog
    uiState.premiumPromotionVoice?.let { voice ->
        PremiumVoicePromotionDialog(
            voice = voice,
            onUpgradeToPro = {
                viewModel.dismissPremiumPromotion()
                onNavigateToSubscription()
            },
            onDismiss = { viewModel.dismissPremiumPromotion() }
        )
    }
}
