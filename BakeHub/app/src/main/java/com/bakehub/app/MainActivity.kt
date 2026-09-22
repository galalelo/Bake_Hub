package com.bakehub.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bakehub.app.ui.navigation.BakeHubNavGraph
import com.bakehub.app.ui.theme.BakeHubTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BakeHubTheme {
                BakeHubNavGraph()
            }
        }
    }
}
