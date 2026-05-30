package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.repository.GrowthRepository
import com.example.ui.screens.MainGrowthScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GrowthViewModel
import com.example.ui.viewmodel.GrowthViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Core Room Database, Repository, WebDAV Sync, and ViewModel Factory
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = GrowthRepository(
            profileDao = database.profileDao(),
            habitItemDao = database.habitItemDao(),
            dailyRecordDao = database.dailyRecordDao(),
            ruleDao = database.ruleDao()
        )
        val syncManager = com.example.data.sync.WebDavSyncManager(applicationContext, repository)
        val factory = GrowthViewModelFactory(repository, syncManager)

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Initialize ViewModel with standard Factory
                val viewModel: GrowthViewModel = viewModel(factory = factory)
                
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    MainGrowthScreen(viewModel = viewModel)
                }
            }
        }
    }
}
