package com.university.campuscare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.university.campuscare.ui.CampusFixApp
import com.university.campuscare.ui.theme.CampusFixTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CampusFixTheme {
                // Main entry point for the Compose app
                CampusFixApp()
            }
        }
    }
}