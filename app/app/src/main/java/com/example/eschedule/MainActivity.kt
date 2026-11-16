package com.example.eschedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.eschedule.theme.EScheduleTheme
import com.example.eschedule.ui.AppScaffold

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EScheduleTheme {
                AppScaffold()
            }
        }
    }
}
