package com.example.contentanalyzer.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.contentanalyzer.presentation.viewmodel.AnalyzerViewModel
import com.example.contentanalyzer.presentation.viewmodel.HistoryViewModel
import com.example.contentanalyzer.presentation.viewmodel.ImageAnalyzerViewModel

sealed class Screen(val route: String) {
    object TextAnalysis : Screen("text_analysis")
    object ImageAnalysis : Screen("image_analysis")
    object History : Screen("history")
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    textViewModel: AnalyzerViewModel = hiltViewModel(),
    imageViewModel: ImageAnalyzerViewModel = hiltViewModel(),
    historyViewModel: HistoryViewModel = hiltViewModel()
) {
    NavGraph(
        navController = navController,
        textViewModel = textViewModel,
        imageViewModel = imageViewModel,
        historyViewModel = historyViewModel,
        modifier = modifier
    )
}
