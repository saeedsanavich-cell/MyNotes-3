package com.example.mynotes

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.foundation.ExperimentalFoundationApi
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private data class Note(val id: String, val title: String, val body: String)

class MainActivity : ComponentActivity() {

    private var saveNotesAction: (() -> Unit)? = null

    fun registerSaveAction(action: () -> Unit) {
        saveNotesAction = action
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NotesScreen(this) }
    }

    override fun onStop() {
        saveNotesAction?.invoke()
        super.onStop()
    }

    override fun onDestroy() {
        saveNotesAction?.invoke()
        super.onDestroy()
    }
}

private fun loadNotes(context: Context): List<Note> {
    val raw = context.getSharedPreferences("notes", Context.MODE_PRIVATE)
        .getString("items", null) ?: return listOf(Note(UUID.randomUUID().toString(), "My first note", ""))
    return try {
        val a = JSONArray(raw)
        (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            Note(o.getString("id"), o.getString("title"), o.optString("body", ""))
        }.ifEmpty { listOf(Note(UUID.randomUUID().toString(), "My first note", "")) }
    } catch (_: Exception) {
        listOf(Note(UUID.randomUUID().toString(), "My first note", ""))
    }
}

private fun persist(context: Context, notes: List<Note>) {
    val a = JSONArray()
    notes.forEach { n ->
        a.put(JSONObject().apply {
            put("id", n.id); put("title", n.title); put("body", n.body)
        })
    }
    context.getSharedPreferences("notes", Context.MODE_PRIVATE)
        .edit().putString("items", a.toString()).apply()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun NotesScreen(context: Context) {
    val notes = remember { mutableStateListOf<Note>().apply { addAll(loadNotes(context)) } }
    val activity = context as? MainActivity

DisposableEffect(Unit) {
    activity?.registerSaveAction {
        persist(context, notes.toList())
    }

    onDispose {
        activity?.registerSaveAction {}
    }
}
    var selectedId by remember { mutableStateOf(notes.first().id) }
    var draft by remember { mutableStateOf(notes.first().body) }
    var dirty by remember { mutableStateOf(false) }
    var pendingSwitch by remember { mutableStateOf<String?>(null) }
    var showUnsaved by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var menuId by remember { mutableStateOf<String?>(null) }
    var renameId by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleteId by remember { mutableStateOf<String?>(null) }
    var toast by remember { mutableStateOf("") }


    fun saveCurrent(showMessage: Boolean = true) {
        val i = notes.indexOfFirst { it.id == selectedId }
        if (i >= 0) notes[i] = notes[i].copy(body = draft)
        persist(context, notes)
        dirty = false
        if (showMessage) toast = "Saved"
    }

   fun switchNow(id: String) {
    if (id == selectedId) return

    val currentIndex = notes.indexOfFirst { it.id == selectedId }

    if (currentIndex >= 0) {
        notes[currentIndex] = notes[currentIndex].copy(body = draft)
        persist(context, notes.toList())
    }

    val n = notes.firstOrNull { it.id == id } ?: return

    selectedId = id
    draft = n.body
    dirty = false
}

    fun askSwitch(id: String) {
        if (id == selectedId) return
        switchNow(id)
    }




    MaterialTheme {
        Scaffold(
            containerColor = Color(0xFFF7F7F7),
            bottomBar = {
                Surface(color = Color.White, shadowElevation = 5.dp) {
                    Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 8.dp, bottom = 8.dp)) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("NOTES", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { newTitle = ""; showNew = true }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Text("New Note")
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            notes.forEach { note ->
                                Box {
                                    Surface(
                                        color = if (note.id == selectedId) Color(0xFFDBEAFE) else Color(0xFFF0F0F0),
                                        shape = RoundedCornerShape(9.dp),
                                        modifier = Modifier.combinedClickable(
                                            onClick = { askSwitch(note.id) },
                                            onLongClick = { menuId = note.id }
                                        )
                                    ) {
                                        Row(Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically) {
                                            Text(note.title, color = if (note.id == selectedId) Color(0xFF1D4ED8) else Color.DarkGray,
                                                maxLines = 1, fontSize = 13.sp)
                                            if (note.id == selectedId && dirty) {
                                                Text(" •", color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(notes.firstOrNull { it.id == selectedId }?.title ?: "Note",
                        modifier = Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    if (dirty) Text("Unsaved", color = Color(0xFFB7791F), fontSize = 12.sp)
                }
                Surface(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    color = Color.White,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
                ) {
                    Box(Modifier.fillMaxSize().padding(14.dp)) {
                        if (draft.isEmpty()) Text("Start writing...", color = Color.LightGray)
                        BasicTextField(
                            value = draft,
     onValueChange = {
    draft = it
    dirty = true
    toast = ""

    val i = notes.indexOfFirst { note -> note.id == selectedId }
    if (i >= 0) {
        notes[i] = notes[i].copy(body = it)
    }
},
                            modifier = Modifier.fillMaxSize(),
                           textStyle = TextStyle(
    fontSize = 16.sp,
    color = Color(0xFF222222),
    lineHeight = 24.sp,
    textAlign = TextAlign.Start,
    textDirection = TextDirection.ContentOrRtl
)
                        )
                    }
                }
        
                if (toast.isNotEmpty()) Text(toast, color = Color(0xFF388E3C), fontSize = 12.sp)
            }
        }
    }


    if (showNew) AlertDialog(
        onDismissRequest = { showNew = false },
        title = { Text("New note") },
        text = {
            OutlinedTextField(value = newTitle, onValueChange = { newTitle = it },
                label = { Text("Note title") }, singleLine = true)
        },
        confirmButton = {
            TextButton(onClick = {
                val title = newTitle.trim().ifEmpty { "Untitled" }
                val n = Note(UUID.randomUUID().toString(), title, "")
                notes.add(n); persist(context, notes); switchNow(n.id)
                showNew = false
            }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = { showNew = false }) { Text("Cancel") } }
    )

    menuId?.let { id ->
        val n = notes.firstOrNull { it.id == id }
        if (n != null) AlertDialog(
            onDismissRequest = { menuId = null },
            title = { Text(n.title) },
            text = {
                Column {
                    TextButton(onClick = { renameId = id; renameText = n.title; menuId = null }) { Text("Rename") }
                    TextButton(onClick = {
                        val copy = Note(UUID.randomUUID().toString(), "${n.title} copy", n.body)
                        notes.add(copy); persist(context, notes); menuId = null
                    }) { Text("Duplicate") }
                    TextButton(onClick = { deleteId = id; menuId = null }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = { TextButton(onClick = { menuId = null }) { Text("Close") } }
        )
    }

    renameId?.let { id ->
        AlertDialog(
            onDismissRequest = { renameId = null },
            title = { Text("Rename note") },
            text = { OutlinedTextField(value = renameText, onValueChange = { renameText = it }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    val i = notes.indexOfFirst { it.id == id }
                    if (i >= 0 && renameText.isNotBlank()) {
                        notes[i] = notes[i].copy(title = renameText.trim())
                        persist(context, notes)
                    }
                    renameId = null
                }) { Text("Rename") }
            },
            dismissButton = { TextButton(onClick = { renameId = null }) { Text("Cancel") } }
        )
    }

    deleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteId = null },
            title = { Text("Delete note?") },
            text = { Text("This note will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    if (notes.size > 1) {
                        val wasSelected = selectedId == id
                        notes.removeAll { it.id == id }
                        persist(context, notes)
                        if (wasSelected) switchNow(notes.first().id)
                    } else {
                        notes[0] = notes[0].copy(title = "My first note", body = "")
                        persist(context, notes); switchNow(notes[0].id)
                    }
                    deleteId = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Cancel") } }
        )
    }
}
