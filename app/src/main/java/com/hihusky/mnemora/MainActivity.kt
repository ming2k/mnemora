package com.hihusky.mnemora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.hihusky.mnemora.ui.components.navigation.MnemoraBottomNavigation
import com.hihusky.mnemora.ui.navigation.MnemoraNavHost
import com.hihusky.mnemora.ui.theme.MnemoraTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            MnemoraTheme(themeMode = themeMode) {
                val navController = rememberNavController()
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = { MnemoraBottomNavigation(navController) },
                ) { innerPadding ->
                    MnemoraNavHost(
                        navController = navController,
                        modifier =
                            Modifier
                                .padding(innerPadding)
                                .consumeWindowInsets(innerPadding),
                    )
                }
            }
        }
    }
}
