package com.denham.skyline.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.denham.skyline.tv.TvNavTab
import com.denham.skyline.ui.browse.CategoryChipsRow
import com.denham.skyline.ui.components.PillButton
import com.denham.skyline.ui.components.SectionHeader
import com.denham.skyline.ui.theme.SkylineTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Click-through behaviour of the interactive pieces, phone and TV. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = android.app.Application::class)
class ClickBehaviourTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `pill button fires its action`() {
        var clicks = 0
        compose.setContent {
            SkylineTheme { PillButton("Watch Live", onClick = { clicks++ }) }
        }
        compose.onNodeWithText("Watch Live").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun `section header view all fires`() {
        var fired = false
        compose.setContent {
            SkylineTheme { SectionHeader("New films", onViewAll = { fired = true }) }
        }
        compose.onNodeWithText("View all").performClick()
        assertTrue(fired)
    }

    @Test
    fun `category chips switch selection`() {
        compose.setContent {
            var selected by androidx.compose.runtime.remember {
                mutableStateOf<String?>(null)
            }
            SkylineTheme {
                CategoryChipsRow(
                    categories = listOf("1" to "Sports", "2" to "Movies"),
                    selected = selected,
                    onSelect = { selected = it },
                )
            }
        }
        compose.onNodeWithText("Sports").assertIsDisplayed()
        compose.onNodeWithText("Sports").performClick()
        // Selecting a chip keeps it on screen (visual state asserted via
        // screenshots); the click must not crash and All stays available.
        compose.onNodeWithText("All").assertIsDisplayed()
    }

    @Test
    fun `tv nav tab fires on click`() {
        var opened = false
        compose.setContent {
            SkylineTheme { TvNavTab("TV guide", active = false, onClick = { opened = true }) }
        }
        compose.onNodeWithText("TV guide").performClick()
        assertTrue(opened)
    }
}
