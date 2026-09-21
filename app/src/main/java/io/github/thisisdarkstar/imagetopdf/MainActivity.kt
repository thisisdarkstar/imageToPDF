package io.github.thisisdarkstar.imagetopdf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.thisisdarkstar.imagetopdf.theme.ImageToPdfTheme
import io.github.thisisdarkstar.imagetopdf.ui.ImageToPdfApp
import io.github.thisisdarkstar.imagetopdf.viewmodel.MainViewModel

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
