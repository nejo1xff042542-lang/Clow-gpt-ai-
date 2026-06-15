package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    
    private val _userEvent = MutableStateFlow<String?>(null)
    val userEvent: StateFlow<String?> = _userEvent.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val currentUser get() = auth.currentUser

    fun clearError() { _error.value = null }

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _error.value = "Fields cannot be empty"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                auth.signInWithEmailAndPassword(email, pass).await()
                _userEvent.value = "success"
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _error.value = "Fields cannot be empty"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                auth.createUserWithEmailAndPassword(email, pass).await()
                _userEvent.value = "success"
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        auth.signOut()
        _userEvent.value = "logout"
    }
}
