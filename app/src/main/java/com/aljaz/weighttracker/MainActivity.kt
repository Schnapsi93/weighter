package com.aljaz.weighttracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aljaz.weighttracker.ui.WeightTrackerRoot
import com.aljaz.weighttracker.ui.theme.WeightTrackerTheme

/**
 * The app's only Activity. It just applies the theme and hands
 * everything over to Compose ([WeightTrackerRoot]).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeightTrackerTheme {
                WeightTrackerRoot()
            }
        }
    }
}