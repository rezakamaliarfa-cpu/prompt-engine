package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.ui.PromptOptimizerApp
import com.example.ui.PromptViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    val viewModel = ViewModelProvider(this)[PromptViewModel::class.java]
    
    setContent {
      MyApplicationTheme(dynamicColor = false) {
        PromptOptimizerApp(viewModel = viewModel)
      }
    }
  }
}
