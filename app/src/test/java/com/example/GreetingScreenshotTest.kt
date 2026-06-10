package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.database.GodCategory
import com.example.ui.screens.GodCategoryCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val mockCategory = GodCategory(
      id = 1,
      name = "Lord Shiva",
      description = "The supreme protective transformer deity.",
      thumbnail = "https://images.unsplash.com/photo-1609137144813-7d722de15c7e?auto=format&fit=crop&q=80&w=400",
      defaultColor = "#26C6DA"
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        GodCategoryCard(category = mockCategory, onClick = {})
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
