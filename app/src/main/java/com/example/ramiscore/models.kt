package com.example.ramiscore

import android.net.Uri

data class Player(
    val name: String,
    val total: Int = 0,
    val history: List<Int> = emptyList(),
    val eliminated: Boolean = false,
    val avatarUri: Uri? = null
)

data class UiState(
    val started: Boolean = false,
    val maxScore: Int = 501,
    val players: List<Player> = emptyList(),
    val showNameEditor: Boolean = false,
    val showScoreEditor: Boolean = false,
    val showHistoryDialog: Boolean = false,
    val editingPlayerIndex: Int? = null,
    val historyPlayerIndex: Int? = null,
    val tempText: String? = null
)
