package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.WsfLogisticsViewModel
import com.example.ui.screens.WsfDockOpsApp

class MainActivity : ComponentActivity() {
    private val viewModel: WsfLogisticsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WsfDockOpsApp(viewModel = viewModel)
        }
    }
}
