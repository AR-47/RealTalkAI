package com.example.realtalkai

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.realtalkai.db.ChatDatabase
import com.example.realtalkai.db.ChatEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class Screen {
    VOICE_INPUT,
    HISTORY_TRANSCRIPT
}

class MainActivity : ComponentActivity() {
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var db: ChatDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = ChatDatabase.getDatabase(this)
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        } else {
            Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_LONG).show()
        }
        setContent {
            MaterialTheme {
                if (::speechRecognizer.isInitialized) {
                    MainAppScreen(speechRecognizer, db)
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Speech recognition service is not available on this device.")
                    }
                }
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun MainAppScreen(speechRecognizer: SpeechRecognizer, db: ChatDatabase) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isListening by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var activeConversationId by remember { mutableStateOf(-1L) }
    var chatHistory by remember { mutableStateOf(listOf<ChatMessage>()) }
    var recentChats by remember { mutableStateOf(listOf<ChatEntity>()) }
    var currentScreen by remember { mutableStateOf(Screen.VOICE_INPUT) }

    val startListeningAction = {
        startListening(
            context = context,
            speechRecognizer = speechRecognizer,
            onResult = { spokenText ->
                if (spokenText.isNotBlank()) {
                    val userMsg = ChatMessage(spokenText, Sender.USER)
                    val currentHistory = chatHistory + userMsg
                    chatHistory = currentHistory

                    coroutineScope.launch(Dispatchers.IO) {
                        db.chatDao().insert(ChatEntity(conversationId = activeConversationId, text = userMsg.text, sender = "USER"))

                        val sdf = SimpleDateFormat("E, d MMMM yyyy HH:mm:ss z", Locale.getDefault())
                        val contextualInfo = "Current date and time is: " + sdf.format(Date())
                        val headlines = GPTHelper.fetchTopHeadlines()
                        val fullContext = "$contextualInfo\n$headlines"

                        val reply = GPTHelper.getResponse(currentHistory, fullContext)
                        val aiMsg = ChatMessage(reply, Sender.AI)

                        db.chatDao().insert(ChatEntity(conversationId = activeConversationId, text = aiMsg.text, sender = "AI"))
                        recentChats = db.chatDao().getRecentConversations()

                        launch(Dispatchers.Main) {
                            chatHistory = currentHistory + aiMsg
                            GoogleTTSHelper.speak(context, reply)
                        }
                    }
                }
            },
            onListening = { listening -> isListening = listening }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListeningAction()
        } else {
            Toast.makeText(context, "Microphone permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            recentChats = db.chatDao().getRecentConversations()
            activeConversationId = recentChats.firstOrNull()?.conversationId ?: System.currentTimeMillis()
        }
    }

    LaunchedEffect(activeConversationId) {
        coroutineScope.launch(Dispatchers.IO) {
            if (activeConversationId != -1L) {
                chatHistory = db.chatDao().getMessagesForConversation(activeConversationId).map {
                    ChatMessage(it.text, if (it.sender == "USER") Sender.USER else Sender.AI)
                }
            } else {
                chatHistory = emptyList()
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatHistoryDrawer(
                recentChats = recentChats,
                activeConversationId = activeConversationId,
                onChatSelected = { convoId ->
                    activeConversationId = convoId
                    currentScreen = Screen.HISTORY_TRANSCRIPT
                    coroutineScope.launch { drawerState.close() }
                },
                onNewChat = {
                    activeConversationId = System.currentTimeMillis()
                    currentScreen = Screen.VOICE_INPUT
                    coroutineScope.launch { drawerState.close() }
                },
                onDeleteChat = { convoIdToDelete ->
                    coroutineScope.launch(Dispatchers.IO) {
                        db.chatDao().clearChat(convoIdToDelete)
                        recentChats = db.chatDao().getRecentConversations()
                        if (activeConversationId == convoIdToDelete) {
                            activeConversationId = recentChats.firstOrNull()?.conversationId ?: System.currentTimeMillis()
                        }
                    }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("RealTalkAI") },
                    navigationIcon = {
                        if (currentScreen == Screen.VOICE_INPUT) {
                            IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = "Open Menu")
                            }
                        } else {
                            IconButton(onClick = { currentScreen = Screen.VOICE_INPUT }) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Back to Voice Input")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                db.chatDao().clearChat(activeConversationId)
                                recentChats = db.chatDao().getRecentConversations()
                                launch(Dispatchers.Main) {
                                    chatHistory = emptyList()
                                }
                            }
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Clear Active Chat")
                        }
                    }
                )
            }
        ) { paddingValues ->
            AnimatedContent(
                targetState = currentScreen,
                modifier = Modifier.padding(paddingValues),
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "Screen Animation"
            ) { targetScreen ->
                when (targetScreen) {
                    Screen.VOICE_INPUT -> {
                        VoiceInputScreen(
                            isListening = isListening,
                            onTapToTalk = {
                                if (!isListening) {
                                    val permission = Manifest.permission.RECORD_AUDIO
                                    if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                                        startListeningAction()
                                    } else {
                                        permissionLauncher.launch(permission)
                                    }
                                } else {
                                    speechRecognizer.stopListening()
                                }
                            }
                        )
                    }
                    Screen.HISTORY_TRANSCRIPT -> {
                        ConversationTranscriptScreen(
                            chatMessages = chatHistory
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceInputScreen(modifier: Modifier = Modifier, isListening: Boolean, onTapToTalk: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onTapToTalk
            )
    ) {
        FluidOrbAnimation(isListening = isListening)
    }
}

@Composable
fun ConversationTranscriptScreen(modifier: Modifier = Modifier, chatMessages: List<ChatMessage>) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        reverseLayout = true,
        contentPadding = PaddingValues(8.dp)
    ) {
        if (chatMessages.isEmpty()) {
            item {
                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text("This conversation is empty.")
                }
            }
        } else {
            items(chatMessages.reversed()) { message ->
                ChatBubble(message = message)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ChatHistoryDrawer(
    recentChats: List<ChatEntity>,
    activeConversationId: Long,
    onChatSelected: (Long) -> Unit,
    onNewChat: () -> Unit,
    onDeleteChat: (Long) -> Unit
) {
    ModalDrawerSheet {
        Text("Conversations", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
        HorizontalDivider()
        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.AddComment, contentDescription = "New Chat") },
            label = { Text("New Chat") },
            selected = false,
            onClick = onNewChat
        )
        HorizontalDivider()
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(recentChats) { chat ->
                NavigationDrawerItem(
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = chat.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onDeleteChat(chat.conversationId) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete Chat")
                            }
                        }
                    },
                    selected = chat.conversationId == activeConversationId,
                    onClick = { onChatSelected(chat.conversationId) }
                )
            }
        }
    }
}

private fun startListening(
    context: Context,
    speechRecognizer: SpeechRecognizer,
    onResult: (String) -> Unit,
    onListening: (Boolean) -> Unit
) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
    }
    speechRecognizer.setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) { onListening(true) }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() { onListening(false) }
        override fun onError(error: Int) {
            onListening(false)
            Toast.makeText(context, "Speech Error: $error", Toast.LENGTH_SHORT).show()
        }
        override fun onResults(results: Bundle?) {
            val spokenText = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
            onResult(spokenText)
        }
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    })
    speechRecognizer.startListening(intent)
}