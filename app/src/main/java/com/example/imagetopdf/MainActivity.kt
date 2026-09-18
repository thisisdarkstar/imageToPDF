package com.example.imagetopdf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.imagetopdf.theme.ImageToPdfTheme
import com.example.imagetopdf.ui.ImageToPdfApp
import com.example.imagetopdf.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            ImageToPdfTheme(themeMode = themeMode) {
                ImageToPdfApp(viewModel = viewModel)
            }
        }
    }
}
