package com.revamine.shareready

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray

private val Brand = Color(0xFF4F46E5)
private val Ink = Color(0xFF1B2140)
private val Muted = Color(0xFF68708A)
private val Page = Color(0xFFF6F7FB)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Brand, background = Page, surface = Color.White)) {
                ShareReadyScreen(this)
            }
        }
    }
}

@Composable
private fun ShareReadyScreen(context: Context) {
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var previousOutput by remember { mutableStateOf("") }
    var showHistory by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf(loadHistory(context)) }

    fun applyTransform(transformed: String) {
        previousOutput = output
        output = transformed
        if (transformed.isNotBlank()) {
            history = (listOf(transformed) + history.filterNot { it == transformed }).take(15)
            saveHistory(context, history)
        }
    }

    fun cleanText(source: String): String {
        return source.replace("\r\n", "\n")
            .lines()
            .map { it.replace(Regex("[\\t ]+"), " ").trim() }
            .fold(mutableListOf<String>()) { acc, line ->
                if (line.isEmpty()) {
                    if (acc.isNotEmpty() && acc.last().isNotEmpty()) acc.add("")
                } else acc.add(line)
                acc
            }.joinToString("\n").trim()
    }

    fun copyText(text: String) {
        if (text.isBlank()) {
            Toast.makeText(context, "Pehle text add karo", Toast.LENGTH_SHORT).show()
            return
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("ShareReady text", text))
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun shareText(text: String) {
        if (text.isBlank()) {
            Toast.makeText(context, "Pehle text add karo", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share text with"))
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Page) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier.size(48.dp).background(Brand, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.TextFields, contentDescription = null, tint = Color.White, modifier = Modifier.size(27.dp)) }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("ShareReady", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text("Clean text. Share it anywhere.", fontSize = 13.sp, color = Muted)
                }
                OutlinedButton(onClick = { showHistory = !showHistory }, contentPadding = ButtonDefaults.ContentPadding) {
                    Icon(Icons.Default.History, contentDescription = "History", modifier = Modifier.size(18.dp))
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("YOUR TEXT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Muted, letterSpacing = 1.sp)
                        Spacer(Modifier.weight(1f))
                        Text("${input.length} chars · ${input.trim().split(Regex("\\s+")).count { it.isNotEmpty() }} words", fontSize = 11.sp, color = Muted)
                    }
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        placeholder = { Text("Paste or type your text here…", color = Color(0xFF9AA0B4)) },
                        shape = RoundedCornerShape(15.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        maxLines = 9
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { applyTransform(cleanText(input)) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                            Text("Clean text", fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(onClick = { input = "" }, shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp)); Text("Clear")
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("QUICK ACTIONS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Muted, letterSpacing = 1.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ActionChip("UPPERCASE", Modifier.weight(1f)) { applyTransform(input.uppercase()) }
                    ActionChip("lowercase", Modifier.weight(1f)) { applyTransform(input.lowercase()) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ActionChip("Remove empty lines", Modifier.weight(1f)) {
                        applyTransform(input.replace("\r\n", "\n").lines().filter { it.isNotBlank() }.joinToString("\n"))
                    }
                    ActionChip("Unique lines", Modifier.weight(1f)) {
                        applyTransform(input.replace("\r\n", "\n").lines().distinctBy { it.trim().lowercase() }.joinToString("\n"))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ActionChip("Trim each line", Modifier.weight(1f)) {
                        applyTransform(input.replace("\r\n", "\n").lines().joinToString("\n") { it.trim() })
                    }
                    ActionChip("Undo", Modifier.weight(1f), enabled = previousOutput.isNotEmpty()) {
                        val temp = output; output = previousOutput; previousOutput = temp
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF0FF)), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("READY TO SHARE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Brand, letterSpacing = 1.sp)
                        Spacer(Modifier.weight(1f))
                        Text("${output.length} chars", fontSize = 11.sp, color = Muted)
                    }
                    if (output.isBlank()) {
                        Text("Your cleaned text will appear here.", color = Muted, fontSize = 14.sp, modifier = Modifier.padding(vertical = 20.dp))
                    } else {
                        Text(output, color = Ink, fontSize = 15.sp, lineHeight = 22.sp)
                    }
                    Divider(color = Color(0xFFDADDF7))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { copyText(output) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(6.dp)); Text("Copy")
                        }
                        ElevatedButton(onClick = { shareText(output) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(6.dp)); Text("Share")
                        }
                    }
                }
            }

            if (showHistory) {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Recent text", fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                            Text("Stored on this device", color = Muted, fontSize = 11.sp)
                        }
                        if (history.isEmpty()) Text("No saved results yet.", color = Muted)
                        history.forEachIndexed { index, item ->
                            if (index > 0) Divider(color = Color(0xFFE8EAF2))
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { input = item; output = item }) {
                                Text(item, maxLines = 2, color = Ink, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                Text("Use", color = Brand, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 10.dp).background(Color(0xFFEEF0FF), RoundedCornerShape(9.dp)).padding(horizontal = 12.dp, vertical = 8.dp))
                            }
                        }
                        OutlinedButton(onClick = { history = emptyList(); saveHistory(context, history) }, modifier = Modifier.fillMaxWidth()) { Text("Clear history") }
                    }
                }
            }

            Text("Works offline · Your text stays on your device", color = Muted, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp))
        }
    }
}

@Composable
private fun ActionChip(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color.White, contentColor = Ink, disabledContentColor = Muted)
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

private fun loadHistory(context: Context): List<String> {
    return try {
        val raw = context.getSharedPreferences("shareready", Context.MODE_PRIVATE).getString("history", "[]") ?: "[]"
        val json = JSONArray(raw)
        (0 until json.length()).mapNotNull { json.optString(it).takeIf(String::isNotBlank) }
    } catch (_: Exception) { emptyList() }
}

private fun saveHistory(context: Context, items: List<String>) {
    val json = JSONArray()
    items.forEach { json.put(it) }
    context.getSharedPreferences("shareready", Context.MODE_PRIVATE).edit().putString("history", json.toString()).apply()
}
