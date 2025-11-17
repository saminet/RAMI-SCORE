package com.example.ramiscore

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class GameViewModel : ViewModel() {
    private val gson = Gson()
    private val storageFileName = "rami_state.json"
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun startGame(maxScore: Int, playersCount: Int) {
        val players = mutableListOf<Player>()
        for (i in 1..playersCount) players.add(Player(name = "Joueur $i"))
        _uiState.value = UiState(started = true, maxScore = maxScore, players = players)
        saveGame()
    }

    fun resetGame() {
        _uiState.value = UiState()
    }

    fun editPlayerName(index: Int) {
        _uiState.value = _uiState.value.copy(showNameEditor = true, editingPlayerIndex = index, tempText = _uiState.value.players[index].name)
    }

    fun editScoreForPlayer(index: Int) {
        _uiState.value = _uiState.value.copy(showScoreEditor = true, editingPlayerIndex = index, tempText = "0")
    }

    fun updateTempText(t: String) {
        _uiState.value = _uiState.value.copy(tempText = t)
    }

    fun confirmNameEdit() {
        val s = _uiState.value
        val idx = s.editingPlayerIndex ?: return
        val name = s.tempText ?: return
        val players = s.players.toMutableList()
        players[idx] = players[idx].copy(name = name)
        _uiState.value = s.copy(players = players, showNameEditor = false, tempText = null)
        saveGame()
    }

    fun confirmScoreEdit() {
        val s = _uiState.value
        val idx = s.editingPlayerIndex ?: return
        val add = (s.tempText ?: "0").toIntOrNull() ?: 0
        val players = s.players.toMutableList()
        val p = players[idx]
        val newTotal = p.total + add
        val newHistory = p.history.toMutableList()
        newHistory.add(add)
        players[idx] = p.copy(total = newTotal, history = newHistory, eliminated = newTotal >= s.maxScore)
        _uiState.value = s.copy(players = players, showScoreEditor = false, tempText = null)
        checkEliminations()
        saveGame()
    }

    fun toggleHistoryDialog(index: Int) {
        _uiState.value = _uiState.value.copy(showHistoryDialog = true, historyPlayerIndex = index)
    }

    fun dismissDialogs() {
        _uiState.value = _uiState.value.copy(showNameEditor = false, showScoreEditor = false, showHistoryDialog = false, tempText = null)
    }

    fun nextRound() {
        // simply move on; nothing special here in this simple implementation
        saveGame()
    }

    fun saveGame() {
        viewModelScope.launch {
            // save to app-specific files dir if available; fallback: ignore
            // For unit test / emulation purposes, we serialize state and attempt to save to /data/data/... not accessible here.
            // Real persistence should use DataStore or Room; for brevity we use JSON string return from getSerializedState()
        }
    }

    fun getSerializedState(): String = gson.toJson(_uiState.value)

    private fun checkEliminations() {
        val s = _uiState.value
        val remaining = s.players.count { !it.eliminated }
        if (remaining == 1) {
            // we could mark winner; for brevity leave it in state
        }
    }

    // Image helpers (for avatar selection)
    fun pickAvatar(index: Int) { /* handled by Activity in real app via contract */ }

    fun getBitmapFromUri(context: Context, uri: Uri?): Bitmap? {
        if (uri == null) return null
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val src = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(src)
            } else {
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            null
        }
    }
}
