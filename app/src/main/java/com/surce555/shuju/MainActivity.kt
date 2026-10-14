package com.surce555.shuju

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.surce555.shuju.ui.MainScreen
import com.surce555.shuju.ui.theme.ShujuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShujuTheme {
                MainScreen()
            }
        }
    }
}
