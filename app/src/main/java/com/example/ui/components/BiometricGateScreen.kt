package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.utils.L10n

@Composable
fun BiometricGateScreen(
    langCode: String,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pinText by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    val defaultPin = "1234"

    fun handleKeyPress(char: Char) {
        if (pinText.length < 4) {
            val newPin = pinText + char
            pinText = newPin
            pinError = false
            if (newPin == defaultPin) {
                onSuccess()
            } else if (newPin.length == 4) {
                pinError = true
                pinText = "" // Clear to try again
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Secured",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = L10n.get("biometric_locked", langCode),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("biometric_locked_title")
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (pinError) L10n.get("incorrect_pin", langCode) else L10n.get("enter_pin", langCode) + " (Try: 1234)",
            color = if (pinError) Color.Red else Color.White.copy(alpha = 0.5f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(32.dp))

        // DOTS indicator
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 48.dp)
        ) {
            repeat(4) { idx ->
                val filled = idx < pinText.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (filled) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f)
                        )
                )
            }
        }

        // Numeric Keypad
        val keys = listOf(
            listOf('1', '2', '3'),
            listOf('4', '5', '6'),
            listOf('7', '8', '9'),
            listOf('⌫', '0', '⚡')
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            keys.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { key ->
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.04f))
                                .clickable {
                                    when (key) {
                                        '⌫' -> {
                                            if (pinText.isNotEmpty()) {
                                                pinText = pinText.dropLast(1)
                                            }
                                        }
                                        '⚡' -> {
                                            // Simulated quick biometric touch trigger
                                            onSuccess()
                                        }
                                        else -> {
                                            handleKeyPress(key)
                                        }
                                    }
                                }
                                .testTag("keypad_$key"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (key == '⚡') {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Simulate Fingerprint Scan",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            } else {
                                Text(
                                    text = key.toString(),
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
