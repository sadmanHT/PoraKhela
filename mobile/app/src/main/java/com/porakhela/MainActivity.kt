package com.porakhela

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import com.porakhela.ui.navigation.PorakhelaNavigation
import com.porakhela.ui.theme.PorakhelaTheme
import com.porakhela.ui.viewmodel.MainViewModel

/**
 * Main Activity for Porakhela App
 * Child-friendly educational platform
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val mainViewModel: MainViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen
        val splashScreen = installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // Keep splash screen visible until app is ready
        splashScreen.setKeepOnScreenCondition {
            !mainViewModel.isReady.value
        }
        
        setContent {
            PorakhelaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isReady by mainViewModel.isReady.collectAsStateWithLifecycle()
                    val isLoggedIn by mainViewModel.isLoggedIn.collectAsStateWithLifecycle()
                    
                    if (isReady) {
                        PorakhelaNavigation(
                            isLoggedIn = isLoggedIn,
                            onLoginStateChange = { loggedIn ->
                                mainViewModel.updateLoginState(loggedIn)
                            }
                        )
                    }
                }
            }
        }
    }
}