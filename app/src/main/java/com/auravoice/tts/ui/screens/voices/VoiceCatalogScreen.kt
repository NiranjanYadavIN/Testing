package com.auravoice.tts.ui.screens.voices

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auravoice.tts.data.model.VoiceModel
import com.auravoice.tts.ui.components.PremiumVoicePromotionDialog
import com.auravoice.tts.ui.components.VoiceCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCatalogScreen(
    viewModel: VoiceCatalogViewModel,
    onSelectVoiceAndNavigate: ((VoiceModel) -> Unit)? = null,
    onNavigateToSubscription: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val availableCountries by viewModel.availableCountries.collectAsStateWithLifecycle()
    val availableLanguages by viewModel.availableLanguages.collectAsStateWithLifecycle()
    val filteredVoices by viewModel.filteredVoices.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()

    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }

    val languagesList = remember(availableLanguages) {
        availableLanguages
            .filter { !it.equals("All Languages", ignoreCase = true) }
            .distinct()
    }

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
                .testTag("voice_catalog_screen")
        ) {
        // Header with Title & Search Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Voice Catalog",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = {
                    if (isSearchExpanded) {
                        viewModel.onSearchQueryChange("")
                    }
                    isSearchExpanded = !isSearchExpanded
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .testTag("btn_toggle_search")
            ) {
                Icon(
                    imageVector = if (isSearchExpanded || uiState.searchQuery.isNotEmpty()) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = "Search Voices",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Error Banner
        uiState.errorMessage?.let { errorMsg ->
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMsg,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(
                        onClick = { viewModel.clearError() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Expandable Search Input
        AnimatedVisibility(visible = isSearchExpanded || uiState.searchQuery.isNotEmpty()) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_search_input"),
                    placeholder = { Text("Search voice by name or language...", color = MaterialTheme.colorScheme.tertiary) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.secondary) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Styled Filter Panel Box enclosing the 3 filters & controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                // 3 Filter Dropdowns in a single horizontal row: [Country] [Dynamic Language] [Gender]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Country Dropdown Filter
                    FilterDropdownButton(
                        label = "Country",
                        selectedText = uiState.selectedCountryFilter,
                        icon = Icons.Default.Public,
                        options = availableCountries,
                        onSelectOption = { country ->
                            viewModel.onFilterCountry(country)
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "filter_country_dropdown"
                    )

                    // 2. Dynamic Language Dropdown Filter
                    FilterDropdownButton(
                        label = "Language",
                        selectedText = if (uiState.selectedLanguageFilter.equals("All", ignoreCase = true)) "All" else uiState.selectedLanguageFilter,
                        icon = Icons.Default.Translate,
                        options = languagesList,
                        onSelectOption = { lang ->
                            viewModel.onFilterLanguage(lang)
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "filter_language_dropdown"
                    )

                    // 3. Gender Dropdown Filter
                    FilterDropdownButton(
                        label = "Gender",
                        selectedText = when (uiState.selectedGenderFilter.uppercase()) {
                            "MALE" -> "Male"
                            "FEMALE" -> "Female"
                            else -> "All"
                        },
                        icon = Icons.Default.Person,
                        options = listOf("All", "Male", "Female"),
                        onSelectOption = { gender ->
                            val code = when (gender) {
                                "Male" -> "MALE"
                                "Female" -> "FEMALE"
                                else -> "ALL"
                            }
                            viewModel.onFilterGender(code)
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "filter_gender_dropdown"
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Access Tier Selector & Reset Filters Option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Access:",
                            color = MaterialTheme.colorScheme.tertiary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                .padding(2.dp)
                        ) {
                            FilterPill(
                                label = "All",
                                isSelected = uiState.selectedTierFilter == "ALL",
                                onClick = { viewModel.onFilterTier("ALL") },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            FilterPill(
                                label = "Free",
                                isSelected = uiState.selectedTierFilter == "FREE",
                                onClick = { viewModel.onFilterTier("FREE") },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            FilterPill(
                                label = "Pro",
                                isSelected = uiState.selectedTierFilter == "PREMIUM",
                                onClick = { viewModel.onFilterTier("PREMIUM") },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }

                    if (uiState.selectedCountryFilter != "All" || uiState.selectedLanguageFilter != "All" || uiState.selectedGenderFilter != "ALL" || uiState.selectedTierFilter != "ALL" || uiState.searchQuery.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                                .clickable { viewModel.resetFilters() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("btn_reset_filters")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Reset",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Results Summary Count Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredVoices.size} Voices Available",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Voices List / Empty State
        if (filteredVoices.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "No Voices",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Voices Match Selected Filters",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Try changing language, gender, or tier filters.",
                        color = MaterialTheme.colorScheme.tertiary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.resetFilters() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Reset All Filters", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filteredVoices, key = { it.id }) { voice ->
                    val isSelected = uiState.selectedVoiceId == voice.id
                    val isPlayingSample = uiState.activePreviewVoiceId == voice.id && playbackState.isPlaying
                    val isBufferingSample = uiState.activePreviewVoiceId == voice.id && playbackState.isBuffering

                    VoiceCard(
                        voice = voice,
                        isSelected = isSelected,
                        isPlayingSample = isPlayingSample,
                        isBufferingSample = isBufferingSample,
                        onSelectVoice = { selectedVoice ->
                            viewModel.selectVoice(selectedVoice)
                            if (!selectedVoice.isPremium || userProfile.isPremium) {
                                onSelectVoiceAndNavigate?.invoke(selectedVoice)
                            }
                        },
                        onPreviewSample = { viewModel.previewVoiceSample(it) }
                    )
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
}

fun getFlagEmoji(language: String): String {
    val l = language.lowercase()
    return when {
        l == "all" || l == "all countries" -> "🌐"
        l.contains("united states") || l.contains("american") || l.contains("en-us") -> "🇺🇸"
        l.contains("united kingdom") || l.contains("british") || l.contains("en-gb") -> "🇬🇧"
        l.contains("india") || l.contains("hindi") || l.contains("punjabi") || l.contains("tamil") ||
                l.contains("telugu") || l.contains("marathi") || l.contains("bengali") || l.contains("gujarati") ||
                l.contains("kannada") || l.contains("malayalam") || l.contains("urdu") || l.contains("assamese") -> "🇮🇳"
        l.contains("spain") || l.contains("spanish") || l.contains("es-es") -> "🇪🇸"
        l.contains("france") || l.contains("french") || l.contains("fr-fr") -> "🇫🇷"
        l.contains("germany") || l.contains("german") || l.contains("de-de") -> "🇩🇪"
        l.contains("japan") || l.contains("japanese") || l.contains("ja-jp") -> "🇯🇵"
        l.contains("china") || l.contains("chinese") || l.contains("zh-cn") || l.contains("mandarin") -> "🇨🇳"
        l.contains("brazil") || l.contains("portuguese") || l.contains("pt-br") -> "🇧🇷"
        l.contains("italy") || l.contains("italian") || l.contains("it-it") -> "🇮🇹"
        l.contains("korea") || l.contains("korean") || l.contains("ko-kr") -> "🇰🇷"
        l.contains("canada") || l.contains("canadian") -> "🇨🇦"
        l.contains("australia") || l.contains("australian") -> "🇦🇺"
        l.contains("russia") || l.contains("russian") -> "🇷🇺"
        l.contains("mexico") || l.contains("mexican") -> "🇲🇽"
        l.contains("saudi") || l.contains("arabic") -> "🇸🇦"
        l.contains("portugal") -> "🇵🇹"
        l.contains("netherlands") || l.contains("dutch") -> "🇳🇱"
        l.contains("turkey") || l.contains("turkish") -> "🇹🇷"
        l.contains("indonesia") || l.contains("indonesian") -> "🇮🇩"
        l.contains("vietnam") || l.contains("vietnamese") -> "🇻🇳"
        l.contains("thailand") || l.contains("thai") -> "🇹🇭"
        l.contains("sweden") || l.contains("swedish") -> "🇸🇪"
        l.contains("poland") || l.contains("polish") -> "🇵🇱"
        l.contains("english") -> "🇺🇸"
        else -> "🌐"
    }
}

@Composable
private fun CountryChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val flagEmoji = getFlagEmoji(label)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = flagEmoji,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun LanguageChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val flagEmoji = getFlagEmoji(label)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = flagEmoji,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun FilterDropdownButton(
    label: String,
    selectedText: String,
    icon: ImageVector,
    options: List<String>,
    onSelectOption: (String) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val isActive = !selectedText.equals("All", ignoreCase = true)

    val filteredOptions = remember(options, searchQuery) {
        if (searchQuery.isBlank()) {
            options
        } else {
            options.filter {
                it.contains(searchQuery, ignoreCase = true) ||
                        getFlagEmoji(it).contains(searchQuery)
            }
        }
    }

    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable {
                    searchQuery = ""
                    expanded = true
                }
                .testTag(testTag),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isActive) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            border = BorderStroke(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = label,
                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isActive) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Open $label dropdown",
                            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val flag = getFlagEmoji(selectedText)
                    if (flag != "🌐" || label == "Country") {
                        Text(text = flag, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(3.dp))
                    }
                    Text(
                        text = selectedText,
                        color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .widthIn(min = 160.dp, max = 220.dp)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            if (options.size > 5) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search...", fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(13.dp)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            Column(
                modifier = Modifier
                    .heightIn(max = 240.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (filteredOptions.isEmpty()) {
                    Text(
                        text = "No options",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(12.dp)
                    )
                } else {
                    filteredOptions.forEach { option ->
                        val isItemSelected = option.equals(selectedText, ignoreCase = true) ||
                                (option == "All" && selectedText == "All")
                        val flag = getFlagEmoji(option)

                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        if (flag != "🌐" || label == "Country") {
                                            Text(text = flag, fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = option,
                                            fontSize = 12.sp,
                                            fontWeight = if (isItemSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isItemSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (isItemSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                onSelectOption(option)
                                expanded = false
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
