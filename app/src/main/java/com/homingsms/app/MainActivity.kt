package com.homingsms.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.homingsms.app.core.designsystem.theme.HomingTheme
import com.homingsms.app.ui.navigation.AppViewModel
import com.homingsms.app.ui.navigation.HomingApp
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handle(intent)
        setContent {
            HomingTheme {
                HomingApp(startRoute = appViewModel.startRoute, routes = appViewModel.routes, onExit = ::finish)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) appViewModel.onLink(intent.dataString)
    }
}
