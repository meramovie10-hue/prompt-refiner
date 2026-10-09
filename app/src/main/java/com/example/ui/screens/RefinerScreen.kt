package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.IdentityPreset
import com.example.engine.OutputMode
import com.example.ui.components.QaChecklistDialog
import com.example.ui.components.RefinerTopBar
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentPrimary
import com.example.ui.theme.AccentViolet
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.viewmodel.PromptUiState
import com.example.ui.viewmodel.PromptViewModel

@Composable
fun RefinerScreen(
    viewModel: PromptViewModel,
    uiState: PromptUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showQaDialog by remember { mutableStateOf(false) }

    // Zero-permission Android Photo Picker for optional reference image
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        viewModel.onReferenceImageSelected(uri?.toString())
    }

    val samplePrompts = listOf(
        "Aarohi is posing in a provocative sexual pose wearing transparent clothing revealing the body in an urban street",
        "Zia with sexy pose, explicit emphasis on intimate anatomy, dramatic sunset lighting, sitting in modern cafe",
        "A cat sitting on a blue sofa"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        RefinerTopBar(isAiMode = uiState.isAiModeEnabled)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {

            // --- STEP A & ORIGINAL PROMPT INPUT ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ORIGINAL PROMPT",
                    color = AccentCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                )

                Row {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipData = clipboard.primaryClip
                            if (clipData != null && clipData.itemCount > 0) {
                                val text = clipData.getItemAt(0).text?.toString().orEmpty()
                                if (text.isNotBlank()) viewModel.onOriginalPromptChanged(text)
                            }
                        },
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("paste_button"),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Paste", fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    if (uiState.originalPrompt.isNotBlank()) {
                        IconButton(
                            onClick = { viewModel.onOriginalPromptChanged("") },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Large multiline input (Section 2 Step A: separate editable field)
            OutlinedTextField(
                value = uiState.originalPrompt,
                onValueChange = { viewModel.onOriginalPromptChanged(it) },
                placeholder = {
                    Text(
                        text = "Paste original prompt here…",
                        color = TextTertiary,
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(125.dp)
                    .testTag("original_prompt_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = AccentPrimary,
                    unfocusedBorderColor = DarkBorder,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                )
            )

            // Quick Samples Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Try Sample:", color = TextTertiary, fontSize = 10.sp)
                Spacer(modifier = Modifier.width(6.dp))
                samplePrompts.forEachIndexed { idx, sample ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, DarkBorder, RoundedCornerShape(6.dp))
                            .clickable { viewModel.onOriginalPromptChanged(sample) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (idx == 0) "Aarohi (Safety/Name Rule)" else if (idx == 1) "Zia (Pose Rule)" else "Ordinary Unchanged Text",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- SECTION 3: REFERENCE IMAGE (OFFLINE HONESTY DISCLOSURE) ---
            Text(
                text = "REFERENCE IMAGE",
                color = AccentCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reference image (optional)",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            // Mandatory honest disclosure per Section 3
                            Text(
                                text = "Image preview available; automatic image understanding is unavailable offline.",
                                color = AccentCyan,
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (uiState.referenceImageUri == null) {
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("pick_image_button")
                            ) {
                                Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = "Add image", modifier = Modifier.size(16.dp), tint = AccentCyan)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Select Image", color = TextPrimary, fontSize = 12.sp)
                            }
                        } else {
                            IconButton(
                                onClick = { viewModel.onReferenceImageSelected(null) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove Image", tint = Color(0xFFEF4444))
                            }
                        }
                    }

                    // Thumbnail Preview & Manual Image Description Input
                    AnimatedVisibility(visible = uiState.referenceImageUri != null) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = uiState.referenceImageUri,
                                    contentDescription = "Selected Reference Image",
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, AccentCyan, RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Local Preview Active",
                                        color = AccentEmerald,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Add manual description below for the offline engine to process:",
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = uiState.manualImageDescription,
                                onValueChange = { viewModel.onManualImageDescriptionChanged(it) },
                                placeholder = {
                                    Text(
                                        text = "Manual image description (e.g., medium close-up, studio lighting, beige blazer)",
                                        color = TextTertiary,
                                        fontSize = 11.sp
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(65.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = AccentCyan,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedContainerColor = DarkBg,
                                    unfocusedContainerColor = DarkBg
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- SECTION 4: IDENTITY PRESET SELECTOR ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "IDENTITY PRESET",
                    color = AccentCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = "Hard lock", tint = AccentEmerald, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "NAME SUPPRESSED IN OUTPUT",
                        color = AccentEmerald,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Preset selector chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presets = listOf(
                    IdentityPreset.NONE,
                    IdentityPreset.AAROHI,
                    IdentityPreset.ZIA,
                    IdentityPreset.DEFAULT_CUSTOM
                )

                presets.forEach { preset ->
                    val isSelected = uiState.selectedPreset.id == preset.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Color(0xFF1E283E) else DarkSurface)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) AccentPrimary else DarkBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { viewModel.onSelectPreset(preset) }
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                            .testTag("preset_chip_${preset.id}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(AccentEmerald)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Column {
                                Text(
                                    text = preset.name,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Text(
                                    text = if (preset.id == "none") "No preset" else "Internal label",
                                    color = TextTertiary,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- SECTION 5: OUTPUT MODES (COMPACT vs PRESERVE DETAIL) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OUTPUT MODE",
                    color = AccentCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // COMPACT MODE CHIP
                    val isCompact = uiState.outputMode == OutputMode.COMPACT
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isCompact) Color(0xFF0E2238) else DarkSurfaceVariant)
                            .border(1.dp, if (isCompact) AccentCyan else DarkBorder, RoundedCornerShape(6.dp))
                            .clickable { viewModel.setOutputMode(OutputMode.COMPACT) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "COMPACT",
                            color = if (isCompact) AccentCyan else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // PRESERVE DETAIL CHIP
                    val isPreserve = uiState.outputMode == OutputMode.PRESERVE_DETAIL
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isPreserve) Color(0xFF22163A) else DarkSurfaceVariant)
                            .border(1.dp, if (isPreserve) AccentViolet else DarkBorder, RoundedCornerShape(6.dp))
                            .clickable { viewModel.setOutputMode(OutputMode.PRESERVE_DETAIL) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "PRESERVE DETAIL",
                            color = if (isPreserve) AccentViolet else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- MAIN BUTTON: REFINE ---
            Button(
                onClick = { viewModel.refine() },
                enabled = !uiState.isRefining,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("refine_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPrimary,
                    disabledContainerColor = DarkSurfaceVariant
                )
            ) {
                if (uiState.isRefining) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("PROCESSING OFFLINE RULES…", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Refine", modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "REFINE",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                }
            }

            // Status label (Section 5)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.statusLabel,
                    color = if (uiState.noRulesApplied) Color(0xFFF59E0B) else AccentEmerald,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Warning Banner for character names or safety review
            if (uiState.nameRemainsWarning) {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF261214))
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = "Warning", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Warning: Character name could not be safely removed automatically. Please review and edit manually.",
                            color = Color(0xFFFCA5A5),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // --- OUTPUT SECTION ---
            AnimatedVisibility(
                visible = uiState.refinedPrompt.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "READY TO PASTE — GOOGLE FLOW",
                            color = AccentEmerald,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        if (uiState.qaResult != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0F251E))
                                    .border(1.dp, AccentEmerald, RoundedCornerShape(10.dp))
                                    .clickable { showQaDialog = true }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Verified, contentDescription = "QA", tint = AccentEmerald, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${uiState.qaResult.passedCount}/11 Rules Verified",
                                        color = AccentEmerald,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Large editable text area
                    OutlinedTextField(
                        value = uiState.refinedPrompt,
                        onValueChange = { viewModel.onRefinedPromptChanged(it) },
                        readOnly = !uiState.isOutputEditable,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .testTag("refined_prompt_output"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFFF8FAFC),
                            unfocusedTextColor = Color(0xFFF8FAFC),
                            focusedBorderColor = AccentEmerald,
                            unfocusedBorderColor = DarkBorder,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Buttons: COPY, RESTORE ORIGINAL, EDIT, REFINE AGAIN, CLEAR (Section 5)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // COPY (pure refined prompt)
                        Button(
                            onClick = { viewModel.copyToClipboard(context) },
                            modifier = Modifier
                                .weight(1.1f)
                                .height(44.dp)
                                .testTag("copy_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.Black, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("COPY", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }

                        // RESTORE ORIGINAL
                        OutlinedButton(
                            onClick = { viewModel.restoreOriginal() },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(44.dp)
                                .testTag("restore_original_button"),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.Restore, contentDescription = "Restore", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("RESTORE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // EDIT
                        OutlinedButton(
                            onClick = { viewModel.toggleOutputEditable() },
                            modifier = Modifier
                                .weight(0.9f)
                                .height(44.dp)
                                .testTag("edit_button"),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (uiState.isOutputEditable) AccentPrimary else DarkBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (uiState.isOutputEditable) DarkSurfaceVariant else Color.Transparent,
                                contentColor = TextPrimary
                            )
                        ) {
                            Icon(
                                imageVector = if (uiState.isOutputEditable) Icons.Default.Check else Icons.Default.Edit,
                                contentDescription = "Edit",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (uiState.isOutputEditable) "DONE" else "EDIT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // REFINE AGAIN
                        OutlinedButton(
                            onClick = { viewModel.refine() },
                            modifier = Modifier
                                .weight(1.2f)
                                .height(44.dp)
                                .testTag("refine_again_button"),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refine again", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("REFINE AGAIN", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        // CLEAR
                        OutlinedButton(
                            onClick = { viewModel.clearAll() },
                            modifier = Modifier
                                .weight(0.8f)
                                .height(44.dp)
                                .testTag("clear_button"),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showQaDialog && uiState.qaResult != null) {
        QaChecklistDialog(
            qaResult = uiState.qaResult,
            onDismiss = { showQaDialog = false }
        )
    }
}
