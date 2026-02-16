package com.example.expensetracker.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.expensetracker.ui.addexpense.AddExpenseSheet
import com.example.expensetracker.ui.components.StyledSnackbar
import com.example.expensetracker.ui.home.HomeScreen
import com.example.expensetracker.ui.list.ExpenseListScreen
import com.example.expensetracker.ui.category.CategoryManagementScreen
import com.example.expensetracker.ui.settings.ExportScreen
import com.example.expensetracker.ui.settings.ImportScreen
import com.example.expensetracker.ui.settings.SettingsScreen
import com.example.expensetracker.ui.stats.StatsScreen
import kotlinx.coroutines.launch
import com.example.expensetracker.ui.theme.OnSurfaceSecondary
import com.example.expensetracker.ui.theme.Primary
import com.example.expensetracker.ui.theme.SurfaceCard

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object ExpenseList : Screen("expense_list")
    object Stats : Screen("stats")
    object Settings : Screen("settings")
    object CategoryManagement : Screen("settings/categories")
    object Import : Screen("settings/import")
    object Export : Screen("settings/export")
}

data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, Icons.Outlined.Home, "Home"),
    BottomNavItem(Screen.ExpenseList, Icons.Outlined.Receipt, "Expenses"),
    BottomNavItem(Screen.Stats, Icons.Outlined.PieChart, "Stats"),
    BottomNavItem(Screen.Settings, Icons.Outlined.Settings, "Settings")
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val bottomBarRoutes = setOf(
        Screen.Home.route,
        Screen.ExpenseList.route,
        "stats?startDate={startDate}&endDate={endDate}",
        Screen.Settings.route
    )
    val showBottomBar = currentDestination?.route in bottomBarRoutes

    var showAddExpenseSheet by remember { mutableStateOf(false) }
    var editExpenseId by remember { mutableLongStateOf(-1L) }
    var statsRefreshTrigger by remember { mutableLongStateOf(0L) }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                StyledSnackbar(data)
            }
        },
        bottomBar = {
            if (showBottomBar) {
            NavigationBar(
                containerColor = SurfaceCard,
                tonalElevation = 0.dp
            ) {
                bottomNavItems.forEach { item ->
                    val selected = currentDestination?.route?.let { route ->
                        route == item.screen.route || route.startsWith(item.screen.route + "?")
                    } == true

                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) },
                        selected = selected,
                        onClick = {
                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Primary,
                            selectedTextColor = Primary,
                            unselectedIconColor = OnSurfaceSecondary,
                            unselectedTextColor = OnSurfaceSecondary,
                            indicatorColor = Primary.copy(alpha = 0.1f)
                        )
                    )
                }
            }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(animationSpec = tween(200))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(200))
            }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onAddExpense = {
                        editExpenseId = -1L
                        showAddExpenseSheet = true
                    },
                    onEditExpense = { id ->
                        editExpenseId = id
                        showAddExpenseSheet = true
                    },
                    onImportData = {
                        navController.navigate(Screen.Import.route)
                    },
                    snackbarHostState = snackbarHostState
                )
            }
            composable(Screen.ExpenseList.route) {
                ExpenseListScreen(
                    snackbarHostState = snackbarHostState,
                    onEditExpense = { id ->
                        editExpenseId = id
                        showAddExpenseSheet = true
                    },
                    onDateClicked = { date ->
                        navController.navigate("stats?startDate=$date&endDate=$date")
                    }
                )
            }
            composable(
                route = "stats?startDate={startDate}&endDate={endDate}",
                arguments = listOf(
                    navArgument("startDate") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("endDate") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val startDate = backStackEntry.arguments?.getString("startDate")
                val endDate = backStackEntry.arguments?.getString("endDate")
                StatsScreen(
                    initialStartDate = startDate,
                    initialEndDate = endDate,
                    onEditExpense = { id ->
                        editExpenseId = id
                        showAddExpenseSheet = true
                    },
                    refreshTrigger = statsRefreshTrigger
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToCategories = {
                        navController.navigate(Screen.CategoryManagement.route)
                    },
                    onNavigateToImport = {
                        navController.navigate(Screen.Import.route)
                    },
                    onNavigateToExport = {
                        navController.navigate(Screen.Export.route)
                    },
                    onDataReset = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = true
                            }
                        }
                    },
                    snackbarMessage = { message ->
                        scope.launch {
                            snackbarHostState.showSnackbar(message)
                        }
                    }
                )
            }
            composable(
                Screen.CategoryManagement.route,
                enterTransition = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) + fadeIn(tween(300))
                },
                exitTransition = {
                    fadeOut(tween(300))
                },
                popEnterTransition = {
                    fadeIn(tween(300))
                },
                popExitTransition = {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) + fadeOut(tween(300))
                }
            ) {
                CategoryManagementScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                Screen.Import.route,
                enterTransition = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) + fadeIn(tween(300))
                },
                popExitTransition = {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) + fadeOut(tween(300))
                }
            ) {
                ImportScreen(onBack = { navController.popBackStack() })
            }
            composable(
                Screen.Export.route,
                enterTransition = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) + fadeIn(tween(300))
                },
                popExitTransition = {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) + fadeOut(tween(300))
                }
            ) {
                ExportScreen(onBack = { navController.popBackStack() })
            }
        }
    }

    // Add/Edit expense bottom sheet
    if (showAddExpenseSheet) {
        AddExpenseSheet(
            editExpenseId = if (editExpenseId >= 0) editExpenseId else null,
            onDismiss = { showAddExpenseSheet = false },
            onSaved = { isEdit ->
                showAddExpenseSheet = false
                statsRefreshTrigger++
                scope.launch {
                    snackbarHostState.showSnackbar(
                        if (isEdit) "Expense updated" else "Expense saved"
                    )
                }
            }
        )
    }
}
