package com.example.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.ChatViewModel

@Composable
fun AppNavigation(onToggleTheme: () -> Unit) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    
    val startDest = if (authViewModel.currentUser != null) "chat" else "login"

    NavHost(navController = navController, startDestination = startDest) {
        composable("login") {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToChat = {
                    navController.navigate("chat") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("chat") {
            val chatViewModel: ChatViewModel = viewModel()
            ChatScreen(
                viewModel = chatViewModel,
                onToggleTheme = onToggleTheme,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("chat") { inclusive = true }
                    }
                }
            )
        }
    }
}
