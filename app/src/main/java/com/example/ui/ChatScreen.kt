package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import android.net.Uri
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.data.UserProfile
import com.example.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onLogout: () -> Unit,
    onToggleTheme: () -> Unit
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val currentSessionId by viewModel.currentSessionId.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var showProfileDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var showModelMenu by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).clickable { showProfileDialog = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (userProfile?.photoUrl?.isNotEmpty() == true) {
                        AsyncImage(
                            model = userProfile?.photoUrl,
                            contentDescription = "Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(40.dp).clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                             Text((userProfile?.displayName?.take(1) ?: "C").uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            userProfile?.displayName ?: "Guest User",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Tap to edit profile",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = { 
                        viewModel.startNewChat()
                        scope.launch { drawerState.close() } 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Chat")
                    Spacer(Modifier.width(8.dp))
                    Text("New Chat")
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    "Recent Chats",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(sessions) { session ->
                        NavigationDrawerItem(
                            label = { Text(session.title, maxLines = 1) },
                            selected = session.id == currentSessionId,
                            onClick = { 
                                viewModel.selectSession(session.id)
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
                
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                
                NavigationDrawerItem(
                    label = { Text("Toggle Theme") },
                    selected = false,
                    onClick = onToggleTheme,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Logout") },
                    selected = false,
                    onClick = onLogout,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Box {
                            TextButton(onClick = { showModelMenu = true }) {
                                Text(
                                    if (selectedModel == "gemini-3.1-pro-preview") "Gemini 1.5 Pro" else "Gemini 1.5 Flash",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            DropdownMenu(
                                expanded = showModelMenu,
                                onDismissRequest = { showModelMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Gemini 1.5 Pro (Most Intelligent)") },
                                    onClick = { 
                                        viewModel.setModel("gemini-3.1-pro-preview")
                                        showModelMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Gemini 1.5 Flash (Super Fast)") },
                                    onClick = { 
                                        viewModel.setModel("gemini-3.5-flash")
                                        showModelMenu = false
                                    }
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Ask CLOW GPT anything...") },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 4
                        )
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    viewModel.sendMessage(inputText)
                                    inputText = ""
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                        }
                    }
                }
            }
        ) { paddingValues ->
            if (messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                       Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                             Text("C", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 40.sp)
                        }
                        Spacer(Modifier.height(24.dp))
                        Text("Welcome to CLOW GPT", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "An intelligent AI assistant powered by Gemini API.", 
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    reverseLayout = false
                ) {
                    items(messages) { message ->
                        MessageBubble(message, userProfile)
                        Spacer(Modifier.height(16.dp))
                    }
                    if (isGenerating) {
                        item {
                            Text("Thinking...", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
        }
    }

    if (showProfileDialog) {
        var editName by remember { mutableStateOf(userProfile?.displayName ?: "") }
        var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
        val photoPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                selectedImageUri = uri
            }
        }

        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("Edit Profile") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null || userProfile?.photoUrl?.isNotEmpty() == true) {
                            AsyncImage(
                                model = selectedImageUri ?: userProfile?.photoUrl,
                                contentDescription = "Profile Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Person, contentDescription = "Add Photo", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Display Name") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateProfile(editName, selectedImageUri)
                    showProfileDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MessageBubble(message: com.example.data.Message, userProfile: UserProfile?) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Computer, contentDescription = "AI", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(8.dp))
        }
        
        Box(
            modifier = Modifier
                .weight(1f, fill = false)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                .padding(16.dp)
        ) {
            Text(
                text = message.text,
                color = if (message.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }

        if (isUser) {
            Spacer(Modifier.width(8.dp))
            if (userProfile?.photoUrl?.isNotEmpty() == true) {
                AsyncImage(
                    model = userProfile.photoUrl,
                    contentDescription = "User",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(32.dp).clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = "User", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
