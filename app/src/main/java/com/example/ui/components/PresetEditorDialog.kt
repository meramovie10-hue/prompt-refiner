package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.IdentityPreset
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentPrimary
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.UUID

@Composable
fun PresetEditorDialog(
    presetToEdit: IdentityPreset?,
    onDismiss: () -> Unit,
    onSave: (IdentityPreset) -> Unit
) {
    val isReadOnly = presetToEdit?.isBuiltIn == true && presetToEdit.id != IdentityPreset.DEFAULT_CUSTOM.id

    var name by remember { mutableStateOf(presetToEdit?.name ?: "") }
    var archetype by remember { mutableStateOf(presetToEdit?.subjectArchetype ?: "the referenced subject") }
    var visualFeatures by remember { mutableStateOf(presetToEdit?.visualFeatures ?: "") }
    var forbiddenWords by remember { mutableStateOf(presetToEdit?.forbiddenTokens?.joinToString(", ") ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (presetToEdit == null) "NEW IDENTITY PRESET" else "IDENTITY PRESET SPECS",
                            color = AccentCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isReadOnly) "Built-in Preset (Read-Only Specs)" else "Internal Identity Characteristics",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Hard lock reminder banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF13221C), RoundedCornerShape(8.dp))
                        .border(1.dp, AccentEmerald.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Name Lock",
                        tint = AccentEmerald
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "HARD LOCK: The name above is for internal selection only. It is mathematically purged and never emitted in the prompt output.",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Preset Name
                Text("INTERNAL PRESET LABEL", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (!isReadOnly) name = it },
                    readOnly = isReadOnly,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentPrimary,
                        unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = DarkBg,
                        unfocusedContainerColor = DarkBg
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Subject Archetype
                Text("NATURAL SUBJECT ARCHETYPE", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("(e.g., 'the referenced female subject in her mid-20s')", color = Color(0xFF64748B), fontSize = 10.sp)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = archetype,
                    onValueChange = { if (!isReadOnly) archetype = it },
                    readOnly = isReadOnly,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentPrimary,
                        unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = DarkBg,
                        unfocusedContainerColor = DarkBg
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Visual Features
                Text("APPROVED VISUAL CHARACTERISTICS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("(Skin tone, facial features, eyes, hair texture, proportions)", color = Color(0xFF64748B), fontSize = 10.sp)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = visualFeatures,
                    onValueChange = { if (!isReadOnly) visualFeatures = it },
                    readOnly = isReadOnly,
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentPrimary,
                        unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = DarkBg,
                        unfocusedContainerColor = DarkBg
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (!isReadOnly) {
                    Button(
                        onClick = {
                            val id = presetToEdit?.id ?: UUID.randomUUID().toString()
                            val forbiddenList = forbiddenWords.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
                            if (name.isNotBlank() && !forbiddenList.contains(name)) {
                                forbiddenList.add(name)
                            }
                            val updatedPreset = IdentityPreset(
                                id = id,
                                name = if (name.isNotBlank()) name else "Custom Identity",
                                isBuiltIn = false,
                                subjectArchetype = archetype,
                                visualFeatures = visualFeatures,
                                forbiddenTokens = forbiddenList
                            )
                            onSave(updatedPreset)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save Preset", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBorder),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Done", color = TextPrimary)
                    }
                }
            }
        }
    }
}
