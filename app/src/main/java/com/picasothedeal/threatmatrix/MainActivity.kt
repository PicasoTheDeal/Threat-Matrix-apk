package com.picasothedeal.threatmatrix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.picasothedeal.threatmatrix.data.ApiService
import com.picasothedeal.threatmatrix.data.ThreatRepository
import com.picasothedeal.threatmatrix.ui.FeedScreen
import com.picasothedeal.threatmatrix.ui.ThreatViewModel
import com.picasothedeal.threatmatrix.ui.theme.ThreatMatrixTheme
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {

    private val viewModel: ThreatViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://threat-matrix-api.picasothedealer-com.workers.dev/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                val api = retrofit.create(ApiService::class.java)
                val repo = ThreatRepository(api)
                return ThreatViewModel(application, repo) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)
        setContent {
            ThreatMatrixTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FeedScreen(viewModel = viewModel)
                }
            }
        }
    }
}