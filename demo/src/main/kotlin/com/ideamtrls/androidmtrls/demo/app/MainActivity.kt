package com.ideamtrls.androidmtrls.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/**
 * The catalog app's one activity: edge to edge, around [CatalogRoot] under [InspectionRoot]. The
 * root's `AMTheme` sets the status and navigation bar icons for the applied appearance (the
 * inspection setting, not only the system's).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        LaunchOptions.read(intent)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val inspection = Inspection(getSharedPreferences("inspection", MODE_PRIVATE))

        setContent {
            InspectionRoot(inspection) {
                CatalogRoot()
            }
        }
    }
}
