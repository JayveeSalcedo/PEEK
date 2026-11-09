package com.peek.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.peek.app.features.home.HomeScreen
import com.peek.app.ui.theme.PeekTheme

class MainActivity : ComponentActivity() {

    private val container get() = (application as PeekApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PeekTheme {
                HomeScreen(container)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The overlay permission may have changed in Settings.
        container.refreshEdgeHandle()
    }
}
