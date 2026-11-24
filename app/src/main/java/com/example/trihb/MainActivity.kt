package com.example.trihb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.trihb.ui.navigation.TriHubNavGraph
import com.example.trihb.ui.theme.TriHubTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TriHubTheme {
                TriHubNavGraph()
            }
        }
    }
}
