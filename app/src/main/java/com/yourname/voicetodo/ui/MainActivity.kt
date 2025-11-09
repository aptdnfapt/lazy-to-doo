package com.yourname.voicetodo.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.isSystemInDarkTheme
import com.yourname.voicetodo.ai.transcription.RecorderManager
import com.yourname.voicetodo.data.preferences.UserPreferences
import com.yourname.voicetodo.ui.navigation.BottomNavigationBar
import com.yourname.voicetodo.ui.navigation.NavGraph
import com.yourname.voicetodo.ui.theme.VoiceTodoTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var recorderManager: RecorderManager

    @Inject
    lateinit var userPreferences: UserPreferences

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permission results
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            // You could show a message to the user here
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Check and request permissions
        checkAndRequestPermissions()

        setContent {
            val themeMode by userPreferences.getThemeMode().collectAsState(initial = UserPreferences.ThemeMode.SYSTEM)

            val darkTheme = when (themeMode) {
                UserPreferences.ThemeMode.LIGHT -> false
                UserPreferences.ThemeMode.DARK -> true
                UserPreferences.ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            VoiceTodoTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavGraph()
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val requiredPermissions = RecorderManager.requiredPermissions()
        val notGrantedPermissions = requiredPermissions.filter { permission ->
            checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
        }

        if (notGrantedPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(notGrantedPermissions.toTypedArray())
        }
    }
}