package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainScreen
import com.example.ui.theme.FeedMyFlyTheme
import com.example.util.LanguageHelper
import com.example.viewmodel.FlyViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: FlyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var currentLanguage by remember {
                mutableStateOf(LanguageHelper.getSavedLanguage(this@MainActivity))
            }

            val localizedContext = remember(currentLanguage) {
                LanguageHelper.createLocalizedContext(this@MainActivity, currentLanguage)
            }

            CompositionLocalProvider(LocalContext provides localizedContext) {
                FeedMyFlyTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                        MainScreen(
                            viewModel = viewModel,
                            uiState = uiState.copy(currentLanguage = currentLanguage),
                            onLanguageChanged = { newLang ->
                                LanguageHelper.setAppLanguage(this@MainActivity, newLang)
                                currentLanguage = newLang
                            }
                        )
                    }
                }
            }
        }
    }
}
