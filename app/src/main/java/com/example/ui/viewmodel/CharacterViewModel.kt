package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.PromptDatabase
import com.example.data.model.CharacterEntity
import com.example.data.model.PropEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class CharacterViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = PromptDatabase.getInstance(app).characterDao()

    val characters: StateFlow<List<CharacterEntity>> =
        dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun propsFor(id: Long): Flow<List<PropEntity>> = dao.observeProps(id)

    fun load(id: Long, onResult: (CharacterEntity?) -> Unit) {
        viewModelScope.launch { onResult(dao.getById(id)) }
    }

    fun save(
        existing: CharacterEntity?,
        name: String,
        closeUp: String?,
        outfit: String?,
        sheet: String?,
        onDone: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val id = if (existing == null) {
                dao.insert(
                    CharacterEntity(
                        name = name.trim(),
                        closeUpPath = closeUp,
                        outfitPath = outfit,
                        sheetPath = sheet
                    )
                )
            } else {
                dao.update(
                    existing.copy(
                        name = name.trim(),
                        closeUpPath = closeUp,
                        outfitPath = outfit,
                        sheetPath = sheet
                    )
                )
                existing.id
            }
            onDone(id)
        }
    }

    fun delete(c: CharacterEntity) {
        viewModelScope.launch { dao.delete(c) }
    }

    fun addProp(p: PropEntity) {
        viewModelScope.launch { dao.insertProp(p) }
    }

    fun updateProp(p: PropEntity) {
        viewModelScope.launch { dao.updateProp(p) }
    }

    fun deleteProp(p: PropEntity) {
        viewModelScope.launch { dao.deleteProp(p) }
    }

    /** Picked image ko app ki apni storage me copy karta hai, taaki restart ke baad bhi rahe. */
    fun importImage(uri: Uri, tag: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) {
                try {
                    val app = getApplication<Application>()
                    val dir = File(app.filesDir, "characters").apply { mkdirs() }
                    val file = File(dir, "${tag}_${System.currentTimeMillis()}.jpg")
                    val stream = app.contentResolver.openInputStream(uri)
                        ?: return@withContext null
                    stream.use { input ->
                        file.outputStream().use { out -> input.copyTo(out) }
                    }
                    file.absolutePath
                } catch (e: Exception) {
                    null
                }
            }
            onResult(path)
        }
    }
}
