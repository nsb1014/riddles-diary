package com.riddle.diary.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.riddle.diary.ui.diary.DiaryScreen
import com.riddle.diary.ui.diary.DiaryViewModel
import com.riddle.diary.ui.settings.PersonalityScreen
import com.riddle.diary.ui.settings.SettingsScreen

object Routes {
    const val DIARY = "diary"
    const val SETTINGS = "settings"
    const val PERSONALITY = "personality"
}

@Composable
fun DiaryNavHost(diaryViewModel: DiaryViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.DIARY) {
        composable(Routes.DIARY) {
            DiaryScreen(
                viewModel = diaryViewModel,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = diaryViewModel,
                onBack = { navController.popBackStack() },
                onOpenPersonality = { navController.navigate(Routes.PERSONALITY) }
            )
        }
        composable(Routes.PERSONALITY) {
            PersonalityScreen(
                viewModel = diaryViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
