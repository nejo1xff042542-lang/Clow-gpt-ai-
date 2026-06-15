package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ui.AppNavigation
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private var isSystemDarkTheme by mutableStateOf(false)
    private var isThemeInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Read initial theme preference from system.
            if (!isThemeInitialized) {
                isSystemDarkTheme = isSystemInDarkTheme()
                isThemeInitialized = true
            }
            
            MyApplicationTheme(darkTheme = isSystemDarkTheme, dynamicColor = false) {
                AppNavigation(onToggleTheme = {
                    isSystemDarkTheme = !isSystemDarkTheme
                })
            }
        }
    }
}
