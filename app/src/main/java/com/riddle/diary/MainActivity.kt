package com.riddle.diary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.riddle.diary.ui.DiaryNavHost
import com.riddle.diary.ui.diary.DiaryViewModel
import com.riddle.diary.ui.theme.DiaryTheme
import com.riddle.diary.ui.theme.Parchment

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as DiaryApplication
        setContent {
            DiaryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Parchment
                ) {
                    val diaryViewModel: DiaryViewModel = viewModel(
                        factory = DiaryViewModel.Factory(app.container)
                    )
                    DiaryNavHost(diaryViewModel = diaryViewModel)
                }
            }
        }
    }
}
