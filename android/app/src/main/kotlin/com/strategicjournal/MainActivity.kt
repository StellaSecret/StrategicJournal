package com.strategicjournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.strategicjournal.presentation.screens.entry.EntryScreen
import com.strategicjournal.presentation.screens.home.HomeScreen
import com.strategicjournal.presentation.screens.review.ReviewScreen
import com.strategicjournal.presentation.theme.StrategicJournalTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StrategicJournalTheme {
                val navController = rememberNavController()
                AppNavHost(navController)
            }
        }
    }
}

object Routes {
    const val HOME = "home"
    const val ENTRY = "entry?entryId={entryId}"
    const val REVIEW = "review"

    fun entry(entryId: String? = null) =
        if (entryId != null) "entry?entryId=$entryId" else "entry"
}

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToEntry = { id -> navController.navigate(Routes.entry(id)) },
                onNavigateToReview = { navController.navigate(Routes.REVIEW) }
            )
        }

        composable(
            route = Routes.ENTRY,
            arguments = listOf(
                navArgument("entryId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            EntryScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.REVIEW) {
            ReviewScreen(onBack = { navController.popBackStack() })
        }
    }
}
