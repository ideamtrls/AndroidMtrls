package com.ideamtrls.androidmtrls.demo

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect

/**
 * The catalog app's one activity: edge to edge, with the status and navigation bar icons
 * following the applied appearance (the inspection setting, not only the system's), around
 * [CatalogRoot] under [InspectionRoot].
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        LaunchOptions.read(intent)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val inspection = Inspection(getSharedPreferences("inspection", MODE_PRIVATE))

        setContent {
            val dark = inspection.isDark()
            DisposableEffect(dark) {
                val bars = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
                onDispose {}
            }
            InspectionRoot(inspection) {
                CatalogRoot()
            }
        }
    }
}
