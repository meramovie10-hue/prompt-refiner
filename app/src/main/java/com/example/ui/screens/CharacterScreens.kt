package com.example.ui.screens

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CharacterEntity
import com.example.data.model.PropEntity
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextTertiary
import com.example.ui.viewmodel.CharacterViewModel

@Composable
fun FileImage(path: String?, modifier: Modifier = Modifier) {
    val bitmap = remember(path) {
        if (path == null) null else try {
            val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
            BitmapFactory.decodeFile(path, opts)?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(modifier.background(DarkBg), contentAlignment = Alignment.Center) {
            Text("No image", color = TextTertiary, fontSize = 11.sp)
        }
    }
}

@Composable
fun ImageSlot(
    title: String,
    hint: String,
    path: String?,
    vm: CharacterViewModel,
    tag: String,
    protectedSlot: Boolean,
    onChanged: (String?) -> Unit
) {
    var confirm by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.importImage(uri, tag) { p -> if (p != null) onChanged(p) }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            FileImage(path, Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, color = TextPrimary, fontWeight = FontWeight.Bold)
                Text(hint, color = TextTertiary, fontSize = 11.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        if (protectedSlot && path != null) confirm = "replace"
                        else launcher.launch("image/*")
                    }) { Text(if (path == null) "Choose" else "Replace") }
                    if (path != null) {
                        TextButton(onClick = {
                            if (protectedSlot) confirm = "remove" else onChanged(null)
                        }) { Text("Remove") }
                    }
                }
            }
        }
    }

    if (confirm != null) {
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Identity image badalni hai?") },
            text = { Text("Close-up is character ki permanent identity hai. Pakka badalna/hatana hai?") },
            confirmButton = {
                TextButton(onClick = {
                    val action = confirm
                    confirm = null
                    if (action == "replace") launcher.launch("image/*") else onChanged(null)
                }) { Text("Haan") }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun CharactersHost(vm: CharacterViewModel) {
    var mode by rememberSaveable { mutableStateOf("list") }
    var editId by rememberSaveable { mutableStateOf<Long?>(null) }

    if (mode == "edit") {
        BackHandler { mode = "list" }
        CharacterEditScreen(
            vm = vm,
            characterId = editId,
            onSaved = { editId = it },
            onBack = { mode = "list" }
        )
    } else {
        CharacterListScreen(
            vm = vm,
            onAdd = { editId = null; mode = "edit" },
            onOpen = { editId = it; mode = "edit" }
        )
    }
}

@Composable
fun CharacterListScreen(vm: CharacterViewModel, onAdd: () -> Unit, onOpen: (Long) -> Unit) {
    val characters by vm.characters.collectAsState()
    var toDelete by remember { mutableStateOf<CharacterEntity?>(null) }

    Column(Modifier.fillMaxSize().background(DarkBg).padding(16.dp)) {
        Text("Characters", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("+ Add Character") }
        Spacer(Modifier.height(12.dp))

        if (characters.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Abhi koi character nahi hai.\nUpar 'Add Character' dabao.",
                    color = TextTertiary
                )
            }
        } else {
            LazyColumn {
                items(characters, key = { it.id }) { c ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { onOpen(c.id) }
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FileImage(
                                c.closeUpPath,
                                Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                c.name,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { toDelete = c }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentCyan)
                            }
                        }
                    }
                }
            }
        }
    }

    toDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("'${c.name}' delete karna hai?") },
            text = { Text("Character aur uske saare props app se hat jayenge. Phone ki image files wahi rahengi.") },
            confirmButton = {
                TextButton(onClick = { vm.delete(c); toDelete = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun CharacterEditScreen(
    vm: CharacterViewModel,
    characterId: Long?,
    onSaved: (Long) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var existing by remember { mutableStateOf<CharacterEntity?>(null) }
    var name by remember { mutableStateOf("") }
    var closeUp by remember { mutableStateOf<String?>(null) }
    var outfit by remember { mutableStateOf<String?>(null) }
    var sheet by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(characterId) {
        if (characterId != null) {
            vm.load(characterId) { c ->
                existing = c
                if (c != null) {
                    name = c.name
                    closeUp = c.closeUpPath
                    outfit = c.outfitPath
                    sheet = c.sheetPath
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(DarkBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("< Back") }
            Text(
                if (characterId == null) "Add Character" else "Edit Character",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it; error = null },
            label = { Text("Character Name *") },
            singleLine = true,
            isError = error != null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )
        error?.let { Text(it, color = Color(0xFFFF6B6B), fontSize = 12.sp) }
        Spacer(Modifier.height(8.dp))

        ImageSlot(
            "Slot 1: Close-up (Identity)", "Face identity. Locked: badalne se pehle confirm.",
            closeUp, vm, "closeup", true
        ) { closeUp = it }
        ImageSlot(
            "Slot 2: Outfit", "Kapde ka reference. Alag se badal sakte ho.",
            outfit, vm, "outfit", false
        ) { outfit = it }
        ImageSlot(
            "Slot 3: Character Sheet", "Upload karo (generator baad me).",
            sheet, vm, "sheet", false
        ) { sheet = it }

        Spacer(Modifier.height(8.dp))
        Button(
            enabled = !saving,
            onClick = {
                if (name.isBlank()) {
                    error = "Naam zaroori hai"
                } else {
                    saving = true
                    vm.save(existing, name, closeUp, outfit, sheet) { id ->
                        saving = false
                        Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                        onSaved(id)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save") }

        Spacer(Modifier.height(16.dp))
        val savedId = characterId ?: existing?.id
        if (savedId == null) {
            Text(
                "Slot 4 (Props): pehle Save karo, phir props add kar sakte ho.",
                color = TextTertiary,
                fontSize = 12.sp
            )
        } else {
            PropsSection(vm, savedId)
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun PropsSection(vm: CharacterViewModel, characterId: Long) {
    val props by vm.propsFor(characterId).collectAsState(initial = emptyList())
    var showAdd by remember { mutableStateOf(false) }
    var editProp by remember { mutableStateOf<PropEntity?>(null) }
    var deleteProp by remember { mutableStateOf<PropEntity?>(null) }

    Text("Slot 4: Props / Items", color = TextPrimary, fontWeight = FontWeight.Bold)
    TextButton(onClick = { showAdd = true }) { Text("+ Add Prop") }
    if (props.isEmpty()) {
        Text("Abhi koi prop nahi.", color = TextTertiary, fontSize = 12.sp)
    }
    props.forEach { p ->
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                FileImage(p.imagePath, Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.name, color = TextPrimary, fontWeight = FontWeight.Bold)
                    if (p.description.isNotBlank()) {
                        Text(p.description, color = TextTertiary, fontSize = 11.sp)
                    }
                }
                TextButton(onClick = { editProp = p }) { Text("Edit") }
                TextButton(onClick = { deleteProp = p }) { Text("Delete") }
            }
        }
    }

    if (showAdd) PropDialog(vm, characterId, null) { showAdd = false }
    editProp?.let { p -> PropDialog(vm, characterId, p) { editProp = null } }
    deleteProp?.let { p ->
        AlertDialog(
            onDismissRequest = { deleteProp = null },
            title = { Text("'${p.name}' hatana hai?") },
            confirmButton = {
                TextButton(onClick = { vm.deleteProp(p); deleteProp = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteProp = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun PropDialog(
    vm: CharacterViewModel,
    characterId: Long,
    initial: PropEntity?,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var desc by remember { mutableStateOf(initial?.description ?: "") }
    var img by remember { mutableStateOf(initial?.imagePath) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Prop" else "Edit Prop") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (bike, knife...)") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description (optional)") }
                )
                ImageSlot("Prop image", "Optional", img, vm, "prop", false) { img = it }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    if (initial == null) {
                        vm.addProp(
                            PropEntity(
                                characterId = characterId,
                                name = name.trim(),
                                description = desc.trim(),
                                imagePath = img
                            )
                        )
                    } else {
                        vm.updateProp(
                            initial.copy(name = name.trim(), description = desc.trim(), imagePath = img)
                        )
                    }
                    onDismiss()
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
