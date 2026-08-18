package com.rahmatsobrian.sirohaequ

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rahmatsobrian.sirohaequ.logging.AppLogger
import com.rahmatsobrian.sirohaequ.ui.EqualizerViewModel
import com.rahmatsobrian.sirohaequ.ui.MainScreen
import com.rahmatsobrian.sirohaequ.ui.diagnostics.DiagnosticsScreen
import com.rahmatsobrian.sirohaequ.ui.devicetuning.DeviceTuningScreen
import com.rahmatsobrian.sirohaequ.ui.presets.PresetScreen
import com.rahmatsobrian.sirohaequ.ui.settings.SettingsScreen
import com.rahmatsobrian.sirohaequ.ui.theme.SirohaEquTheme

object Routes {
    const val MAIN = "main"
    const val DEVICE_TUNING = "device_tuning"
    const val PRESETS = "presets"
    const val SETTINGS = "settings"
    const val DIAGNOSTICS = "diagnostics"
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: EqualizerViewModel = viewModel()
            val state by viewModel.uiState.collectAsState()

            SirohaEquTheme(
                themeMode = state.themeMode,
                dynamicColorEnabled = state.dynamicColor
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = Routes.MAIN) {
                        composable(Routes.MAIN) {
                            MainScreen(
                                state = state,
                                onNavigateDeviceTuning = { navController.navigate(Routes.DEVICE_TUNING) },
                                onNavigatePresets = { navController.navigate(Routes.PRESETS) },
                                onNavigateSettings = { navController.navigate(Routes.SETTINGS) },
                                onToggleEq = viewModel::setEqEnabled,
                                onPreampChange = { /* wired via SettingsRepository in Settings screen */ }
                            )
                        }
                        composable(Routes.DEVICE_TUNING) {
                            DeviceTuningScreen(
                                state = state,
                                onBandChange = viewModel::updateBand,
                                onSaveProfile = viewModel::saveDeviceProfile,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Routes.PRESETS) {
                            PresetScreen(
                                presets = state.presets,
                                activePresetId = state.activePreset.id,
                                onSelect = viewModel::applyPreset,
                                onSaveAs = viewModel::savePresetAs,
                                onDelete = viewModel::deletePreset,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Routes.SETTINGS) {
                            SettingsScreen(
                                state = state,
                                onThemeModeChange = viewModel::setThemeMode,
                                onDynamicColorChange = viewModel::setDynamicColor,
                                onOpenDiagnostics = { navController.navigate(Routes.DIAGNOSTICS) },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Routes.DIAGNOSTICS) {
                            DiagnosticsScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }

        AppLogger.log("MainActivity", "App started on API ${Build.VERSION.SDK_INT}")
    }
}
