package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.ChatSession
import com.example.data.FirebaseRepository
import com.example.data.Message
import com.example.data.UserProfile
import com.example.model.Content
import com.example.model.GenerateContentRequest
import com.example.model.Part
import com.example.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import android.net.Uri

class ChatViewModel : ViewModel() {
    private val repository = FirebaseRepository()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _sessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val sessions: StateFlow<List<ChatSession>> = _sessions.asStateFlow()

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()
    
    private val _selectedModel = MutableStateFlow("gemini-3.1-pro-preview")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    init {
        viewModelScope.launch {
            _userProfile.value = repository.getUserProfile()
            _sessions.value = repository.getSessions()
            val firstSession = _sessions.value.firstOrNull()
            if (firstSession != null) {
                selectSession(firstSession.id)
            } else {
                startNewChat()
            }
        }
    }

    fun updateProfile(displayName: String, imageUri: Uri?) {
        viewModelScope.launch {
            val newProfile = repository.saveUserProfile(displayName, imageUri)
            if (newProfile != null) {
                _userProfile.value = newProfile
            }
        }
    }

    fun setModel(model: String) {
        _selectedModel.value = model
    }

    fun startNewChat() {
        viewModelScope.launch {
            val newSession = repository.createSession("New Chat")
            _sessions.update { listOf(newSession) + it }
            _currentSessionId.value = newSession.id
            _messages.value = emptyList()
        }
    }

    fun selectSession(sessionId: String) {
        _currentSessionId.value = sessionId
        viewModelScope.launch {
            _messages.value = repository.getMessages(sessionId)
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _isGenerating.value) return

        val sessionId = _currentSessionId.value ?: return

        val userMessage = Message(text = text, role = "user")
        _messages.update { it + userMessage }
        
        viewModelScope.launch { repository.saveMessage(sessionId, userMessage) }
        
        val assistantMessageId = java.util.UUID.randomUUID().toString()
        val emptyAssistantMessage = Message(id = assistantMessageId, text = "", role = "model")
        _messages.update { it + emptyAssistantMessage }

        _isGenerating.value = true

        viewModelScope.launch {
            try {
                // In a real app we pass the history. Here passing only the latest due to length limits
                val requestContents = _messages.value.filter { it.text.isNotBlank() }.map { 
                    Content(role = it.role, parts = listOf(Part(text = it.text)))
                }
                
                val request = GenerateContentRequest(contents = requestContents)
                val apiKey = BuildConfig.GEMINI_API_KEY
                
                val responseBody = withContext(Dispatchers.IO) {
                    RetrofitClient.service.generateContentStream(
                        model = _selectedModel.value,
                        apiKey = apiKey,
                        request = request
                    )
                }

                var fullOutput = ""
                withContext(Dispatchers.IO) {
                    responseBody.byteStream().bufferedReader().use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            try {
                                if (line!!.startsWith("[")) line = line!!.substring(1)
                                if (line!!.endsWith(",")) line = line!!.substring(0, line!!.length - 1)
                                if (line!!.endsWith("]")) line = line!!.substring(0, line!!.length - 1)
                                
                                val chunkText = Json.parseToJsonElement(line!!).jsonObject["candidates"]?.jsonArray
                                    ?.getOrNull(0)?.jsonObject?.get("content")?.jsonObject?.get("parts")?.jsonArray
                                    ?.getOrNull(0)?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
                                
                                if (chunkText.isNotEmpty()) {
                                    fullOutput += chunkText
                                    withContext(Dispatchers.Main) {
                                        _messages.update { current ->
                                            current.map { 
                                                if (it.id == assistantMessageId) {
                                                    it.copy(text = fullOutput)
                                                } else it
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {}
                        }
                    }
                }
                repository.saveMessage(sessionId, emptyAssistantMessage.copy(text = fullOutput))
            } catch (e: Exception) {
                _messages.update { current ->
                    current.map { 
                        if (it.id == assistantMessageId) {
                            it.copy(text = "Error: ${e.localizedMessage}", isError = true)
                        } else it
                    }
                }
            } finally {
                _isGenerating.value = false
            }
        }
    }
}

