package dev.tjpal.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.tjpal.GraphEditorMainScreen
import dev.tjpal.ui.ConfigureNodeScreen
import dev.tjpal.viewmodel.GraphEditorViewModel
import org.koin.compose.viewmodel.koinViewModel

val LocalNavController = compositionLocalOf<NavHostController> {
    error("No LocalNavController provided")
}

@Composable
fun Navigation() {
    val navController = rememberNavController()
    val viewModel: GraphEditorViewModel = koinViewModel()

    CompositionLocalProvider(LocalNavController provides navController) {
        NavHost(
            navController = navController,
            startDestination = MainScreenRoute,
            enterTransition = {
                EnterTransition.None
            },
            exitTransition = {
                ExitTransition.None
            }
        ) {
            composable<MainScreenRoute> {
                GraphEditorMainScreen(viewModel)
            }

            dialog<ConfigureNodeDialogRoute> {
                val nodeId = it.toRoute<ConfigureNodeDialogRoute>().nodeId
                ConfigureNodeScreen(nodeId, viewModel)
            }
        }
    }
}
