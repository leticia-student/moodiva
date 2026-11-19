package com.leticia.moodiva

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.leticia.moodiva.presentation.MoodivaRoot
import com.leticia.moodiva.presentation.theme.MoodivaTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoodivaTheme {
                MoodivaRoot()
            }
        }
    }
}
