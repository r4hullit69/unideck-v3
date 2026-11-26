package com.unideck.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unideck.app.ui.UniDeckApp
import com.unideck.app.ui.UniDeckViewModel
import com.unideck.app.ui.theme.UniDeckTheme

class MainActivity : ComponentActivity() {

    private val viewModel: UniDeckViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val preferences by viewModel.preferences.collectAsStateWithLifecycle()
            UniDeckTheme(preferences = preferences) {
                UniDeckApp(viewModel = viewModel)
            }
        }
    }
}
