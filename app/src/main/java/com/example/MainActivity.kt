package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.MindPlayDatabase
import com.example.data.MindPlayRepository
import com.example.ui.MindPlayApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MindPlayViewModel
import com.example.viewmodel.MindPlayViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Initialize Database & Repository
    val database = MindPlayDatabase.getDatabase(applicationContext)
    val dao = database.mindPlayDao()
    val repository = MindPlayRepository(dao)
    
    // Set up ViewModel Factory
    val factory = MindPlayViewModelFactory(repository)
    val viewModel = ViewModelProvider(this, factory)[MindPlayViewModel::class.java]

    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = androidx.compose.material3.MaterialTheme.colorScheme.background
        ) {
          MindPlayApp(viewModel = viewModel)
        }
      }
    }
  }
}
