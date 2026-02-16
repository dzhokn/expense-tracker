package com.example.expensetracker

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.expensetracker.ui.home.BatteryOptimizationDialog
import com.example.expensetracker.ui.navigation.AppNavigation
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import com.example.expensetracker.util.Constants

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Keep splash visible until data loads (brief, handled by Compose recomposition)
        var isReady = false
        splashScreen.setKeepOnScreenCondition { !isReady }

        enableEdgeToEdge()

        val shouldShowBatteryPrompt = shouldShowBatteryPrompt()

        setContent {
            ExpenseTrackerTheme {
                var showBatteryDialog by remember { mutableStateOf(shouldShowBatteryPrompt) }

                if (showBatteryDialog) {
                    BatteryOptimizationDialog(
                        onOpenSettings = {
                            showBatteryDialog = false
                            markBatteryPromptShown()
                            openBatterySettings()
                        },
                        onSkip = {
                            showBatteryDialog = false
                            markBatteryPromptShown()
                        }
                    )
                }

                AppNavigation()

                // Signal splash screen can dismiss
                isReady = true
            }
        }
    }

    private fun shouldShowBatteryPrompt(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        if (manufacturer != "honor" && manufacturer != "huawei") return false

        val prefs = getSharedPreferences(Constants.PREFS_SETTINGS, MODE_PRIVATE)
        return !prefs.getBoolean(Constants.KEY_BATTERY_PROMPT_SHOWN, false)
    }

    private fun markBatteryPromptShown() {
        getSharedPreferences(Constants.PREFS_SETTINGS, MODE_PRIVATE)
            .edit()
            .putBoolean(Constants.KEY_BATTERY_PROMPT_SHOWN, true)
            .apply()
    }

    private fun openBatterySettings() {
        try {
            val intent = Intent().apply {
                component = android.content.ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                )
            }
            startActivity(intent)
        } catch (_: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (_: Exception) {
                // No battery settings available
            }
        }
    }
}
