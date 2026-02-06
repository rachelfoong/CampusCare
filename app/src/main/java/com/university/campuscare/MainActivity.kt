package com.university.campuscare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.university.campuscare.ui.CampusCareApp
import com.university.campuscare.ui.theme.CampusCareTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CampusCareTheme {
                // Main entry point for the Compose app
                CampusCareApp()
            }
        }
    }
}