package com.example.security

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.components.CircularIconBadge
import com.example.ui.theme.appBlackButtonBorder
import com.example.ui.theme.appBlackButtonColors
import java.security.MessageDigest

object ParentalControlHelper {
    fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(pin.trim().toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(pin: String, storedHash: String): Boolean {
        if (storedHash.isBlank() || pin.isBlank()) return false
        return hashPin(pin) == storedHash
    }
}

@Composable
fun ParentalPinDialog(
    title: String,
    subtitle: String,
    storedPinHash: String,
    isSettingNewPin: Boolean = false,
    onPinConfirmed: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("parental_pin_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularIconBadge(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Parental Lock",
                    containerSize = 52.dp,
                    iconSize = 26.dp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { value ->
                        if (value.length <= 8 && value.all { it.isDigit() }) {
                            pinInput = value
                            errorMessage = null
                        }
                    },
                    label = { Text("Enter 4–8 digit PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("parental_pin_input")
                )
                errorMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("parental_pin_cancel_btn")
                    ) {
                        Text("CANCEL", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            if (isSettingNewPin) {
                                if (pinInput.length < 4) {
                                    errorMessage = "PIN must be at least 4 digits."
                                } else {
                                    onPinConfirmed(ParentalControlHelper.hashPin(pinInput))
                                }
                            } else {
                                if (ParentalControlHelper.verifyPin(pinInput, storedPinHash)) {
                                    onPinConfirmed(storedPinHash)
                                } else {
                                    errorMessage = "Incorrect PIN. Please try again."
                                }
                            }
                        },
                        colors = appBlackButtonColors(),
                        border = appBlackButtonBorder(),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("parental_pin_confirm_btn")
                    ) {
                        Text(
                            text = if (isSettingNewPin) "SAVE PIN" else "UNLOCK",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
