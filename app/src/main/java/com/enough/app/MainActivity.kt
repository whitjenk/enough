package com.enough.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.enough.app.feature.main.EnoughApp
import com.enough.app.ui.theme.EnoughTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            EnoughTheme {
                EnoughApp()
            }
        }
    }
}
