package com.rahmatsobrian.sirohaequ

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainScreenInstrumentationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun mainScreen_showsAppTitle() {
        composeRule.onNodeWithText("Siroha Equ").assertExists()
    }

    @Test
    fun mainScreen_showsEqualizerCard() {
        composeRule.onNodeWithText("Equalizer").assertExists()
    }

    @Test
    fun mainScreen_showsDeviceTuningNavigation() {
        composeRule.onNodeWithText("Device Tuning").assertExists()
    }

    @Test
    fun mainScreen_showsPresetsNavigation() {
        composeRule.onNodeWithText("Presets").assertExists()
    }
}
