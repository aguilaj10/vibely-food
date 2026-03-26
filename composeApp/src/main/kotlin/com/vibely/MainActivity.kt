package com.vibely

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.vibely.navigation.AppNavigation

/**
 * Single-activity entry point for Vibely POS.
 * Hosts the Compose content and delegates all navigation to [AppNavigation].
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface {
                    AppNavigation()
                }
            }
        }
    }
}
