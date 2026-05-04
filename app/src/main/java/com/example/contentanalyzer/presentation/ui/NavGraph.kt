package com.example.contentanalyzer.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.contentanalyzer.presentation.viewmodel.AnalyzerViewModel
import com.example.contentanalyzer.presentation.viewmodel.HistoryViewModel
import com.example.contentanalyzer.presentation.viewmodel.ImageAnalyzerViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    textViewModel: AnalyzerViewModel,
    imageViewModel: ImageAnalyzerViewModel,
    historyViewModel: HistoryViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.TextAnalysis.route,
        modifier = modifier
    ) {
        composable(Screen.TextAnalysis.route) {
            AnalyzerScreen(
                onNavigateToImageAnalysis = { navController.navigate(Screen.ImageAnalysis.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                viewModel = textViewModel
            )
        }
        composable(Screen.ImageAnalysis.route) {
            ImageAnalyzerScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                viewModel = imageViewModel
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(
                viewModel = historyViewModel,
                onBackPressed = { navController.popBackStack() },
                onItemClick = { /* Handle item click if needed */ }
            )
        }
    }
}