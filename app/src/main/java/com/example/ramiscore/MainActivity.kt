package com.example.ramiscore

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ramiscore.ui.theme.RamiScoreTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RamiScoreTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppContent(vm)
                }
            }
        }
    }
}

@Composable
fun AppContent(vm: GameViewModel) {
    val state by vm.uiState.collectAsState()
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()
    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(title = { Text("Rami Score", fontWeight = FontWeight.Bold) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { vm.resetGame() }) {
                Text("Réinitialiser")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            if (!state.started) {
                SetupScreen(state, onStart = { maxScore, players -> vm.startGame(maxScore, players) })
            } else {
                GameScreen(state, vm)
            }
        }
    }
}

@Composable
fun SetupScreen(state: UiState, onStart: (Int, Int) -> Unit) {
    var maxScoreText by remember { mutableStateOf("501") }
    var playersCount by remember { mutableStateOf(2) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Simple logo
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(id = R.drawable.ic_logo_placeholder), contentDescription = null, modifier = Modifier.size(80.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("RAMI", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                Text("Score Keeper", fontSize = 14.sp)
            }
        }
        OutlinedTextField(value = maxScoreText, onValueChange = { maxScoreText = it }, label = { Text("Score max") })
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Nombre de joueurs: ", Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { if (playersCount > 2) playersCount -= 1 }) { Text("-") }
            Text(playersCount.toString(), modifier = Modifier.padding(8.dp))
            IconButton(onClick = { if (playersCount < 4) playersCount += 1 }) { Text("+") }
        }
        Button(onClick = { 
            val ms = maxScoreText.toIntOrNull() ?: 501
            onStart(ms, playersCount)
        }) {
            Text("Démarrer la partie")
        }
    }
}

@Composable
fun GameScreen(state: UiState, vm: GameViewModel) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Score max: ${'$'}{state.maxScore}", fontWeight = FontWeight.Bold)
        state.players.forEachIndexed { index, player ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                    if (player.avatarUri != null) {
                        val bitmap = vm.getBitmapFromUri(context, player.avatarUri)
                        if (bitmap != null) {
                            Image(bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.size(56.dp).clip(CircleShape))
                        } else {
                            Image(painterResource(id = R.drawable.ic_avatar), contentDescription = null, modifier = Modifier.size(56.dp))
                        }
                    } else {
                        Image(painterResource(id = R.drawable.ic_avatar), contentDescription = null, modifier = Modifier.size(56.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(player.name, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, modifier = Modifier.clickable {
                                vm.editPlayerName(index)
                            })
                            Spacer(Modifier.width(8.dp))
                            if (!player.eliminated) {
                                Text("(${player.total})", modifier = Modifier.clickable { vm.editScoreForPlayer(index) })
                            } else {
                                Text("Éliminé", color = MaterialTheme.colors.error)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row {
                            Button(onClick = { vm.toggleHistoryDialog(index) }, modifier = Modifier.padding(end = 8.dp)) { Text("Historique") }
                            Button(onClick = { vm.pickAvatar(index) }) { Text("Avatar") }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row {
            Button(onClick = { vm.nextRound() }, modifier = Modifier.weight(1f)) { Text("Valider tour") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { vm.saveGame() }, modifier = Modifier.weight(1f)) { Text("Sauvegarder") }
        }
        // Dialogs & editors handled by ViewModel state
        if (state.showNameEditor) {
            AlertDialog(onDismissRequest = { vm.dismissDialogs() }, title = { Text("Modifier le nom") }, text = {
                Column { OutlinedTextField(value = state.tempText ?: "", onValueChange = { vm.updateTempText(it) }) }
            }, confirmButton = {
                TextButton(onClick = { vm.confirmNameEdit() }) { Text("OK") }
            })
        }
        if (state.showScoreEditor) {
            AlertDialog(onDismissRequest = { vm.dismissDialogs() }, title = { Text("Modifier le score du tour") }, text = {
                Column { OutlinedTextField(value = state.tempText ?: "0", onValueChange = { vm.updateTempText(it) }) }
            }, confirmButton = {
                TextButton(onClick = { vm.confirmScoreEdit() }) { Text("OK") }
            })
        }
        if (state.showHistoryDialog) {
            val hist = state.players[state.historyPlayerIndex ?: 0].history
            AlertDialog(onDismissRequest = { vm.dismissDialogs() }, title = { Text("Historique") }, text = {
                Column { hist.forEachIndexed { i,s -> Text("Tour ${'$'}{i+1} : ${'$'}s") } }
            }, confirmButton = {
                TextButton(onClick = { vm.dismissDialogs() }) { Text("Fermer") }
            })
        }
    }
}
