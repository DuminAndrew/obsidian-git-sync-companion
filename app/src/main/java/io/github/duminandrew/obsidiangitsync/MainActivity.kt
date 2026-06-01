package io.github.duminandrew.obsidiangitsync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import io.github.duminandrew.obsidiangitsync.ui.SyncScreen
import io.github.duminandrew.obsidiangitsync.ui.SyncViewModel
import io.github.duminandrew.obsidiangitsync.ui.theme.ObsidianGitSyncTheme

class MainActivity : ComponentActivity() {

    private val viewModel: SyncViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            ObsidianGitSyncTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    SyncScreen(viewModel = viewModel)
                }
            }
        }
    }
}
