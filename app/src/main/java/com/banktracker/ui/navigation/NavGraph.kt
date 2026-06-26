package com.banktracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.banktracker.ui.banks.BanksScreen
import com.banktracker.ui.dashboard.DashboardScreen
import com.banktracker.ui.smspicker.SmsPickerScreen
import com.banktracker.ui.transactions.TransactionsScreen

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Banks : Screen("banks")
    object Transactions : Screen("transactions")
    object SmsPicker : Screen("sms_picker")
}

@Composable
fun NavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(onOpenSmsPicker = { navController.navigate(Screen.SmsPicker.route) })
        }
        composable(Screen.Banks.route) { BanksScreen() }
        composable(Screen.Transactions.route) { TransactionsScreen() }
        composable(Screen.SmsPicker.route) {
            SmsPickerScreen(onBack = { navController.popBackStack() })
        }
    }
}
