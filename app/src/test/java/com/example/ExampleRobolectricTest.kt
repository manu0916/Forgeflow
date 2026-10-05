package com.example

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.SaveManager
import com.example.model.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.FactoryViewModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w960dp-h540dp-land-xhdpi")
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        SaveManager(app).clearSave()
    }

    @Test
    fun `menu opens and starts new game and renders 1080p canvas without crashing`() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("main_menu_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("new_game_button").performClick()
        composeTestRule.waitForIdle()

        val confirmNodes = composeTestRule.onAllNodes(androidx.compose.ui.test.hasTestTag("confirm_new_game_button"))
        if (confirmNodes.fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithTag("confirm_new_game_button").performClick()
            composeTestRule.waitForIdle()
        }

        composeTestRule.onNodeWithTag("gameplay_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tutorial_overlay_card").assertIsDisplayed()

        // Force an actual Android View draw pass on a 1920x1080 bitmap using NATIVE graphics mode
        composeTestRule.activityRule.scenario.onActivity { activity ->
            val rootView: View = activity.window.decorView.rootView
            val bitmap = Bitmap.createBitmap(
                rootView.width.coerceAtLeast(1920),
                rootView.height.coerceAtLeast(1080),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            rootView.draw(canvas)
        }

        // Skip tutorial
        composeTestRule.onNodeWithTag("skip_tutorial_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("hud_money_badge").assertIsDisplayed()
        composeTestRule.onNodeWithTag("hud_power_badge").assertIsDisplayed()
        composeTestRule.onNodeWithTag("hud_power_overlay_toggle").assertIsDisplayed()
        composeTestRule.onNodeWithTag("power_grid_overlay_legend").assertIsDisplayed()
    }

    @Test
    fun `full factory simulation pipeline extraction belt smelter power and save load`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = FactoryViewModel(app)
        vm.startNewGameConfirmed()

        val initialState = vm.uiState.value
        assertEquals(AppScreen.GAMEPLAY, initialState.currentScreen)
        assertTrue("Starter power should be > 0", initialState.totalPowerGeneratedKw >= 50)

        // Place Extractor on Iron Vein at (5, 6) facing EAST
        val placedExtractor = vm.placeBuildingAt(BuildingType.EXTRACTOR_MK1, 5, 6, Direction.EAST)
        assertTrue("Extractor should place on Iron Vein", placedExtractor)

        // Place Belts from (6, 6) to (7, 6) into Sell Hub at (8, 6)
        assertTrue(vm.placeBuildingAt(BuildingType.BELT_MK1, 6, 6, Direction.EAST))
        assertTrue(vm.placeBuildingAt(BuildingType.BELT_MK1, 7, 6, Direction.EAST))

        val moneyBeforeSim = vm.uiState.value.money
        // Step simulation so ore is extracted, travels belts, and sells at (8,6)
        repeat(40) {
            vm.stepSimulation(0.25f)
        }

        val stateAfterSale = vm.uiState.value
        assertTrue("Money should increase from automatic sales", stateAfterSale.money > moneyBeforeSim)
        assertTrue("Iron ore sold count should be > 0", (stateAfterSale.itemSoldCounts[ItemType.IRON_ORE] ?: 0) > 0)

        // Replace belt at (6,6) with a Smelter to refine Iron Ore into Iron Ingots ($12 vs $4)
        vm.demolishAt(6, 6)
        assertTrue(vm.placeBuildingAt(BuildingType.SMELTER, 6, 6, Direction.EAST))

        repeat(50) {
            vm.stepSimulation(0.25f)
        }

        val stateAfterSmelt = vm.uiState.value
        assertTrue(
            "Smelter should produce and sell Iron Ingots",
            (stateAfterSmelt.itemProducedCounts[ItemType.IRON_INGOT] ?: 0) > 0
        )

        // Test Splitter and Merger logistics
        assertTrue(vm.placeBuildingAt(BuildingType.SPLITTER, 9, 6, Direction.EAST))
        assertTrue(vm.placeBuildingAt(BuildingType.MERGER, 10, 6, Direction.EAST))

        // Test Research & Biome Expansion
        vm.startResearch("TECH_METALWORKING")
        repeat(30) {
            vm.stepSimulation(0.25f)
        }
        assertTrue("TECH_METALWORKING should be unlocked", "TECH_METALWORKING" in vm.uiState.value.unlockedTechIds)

        // Test Save and Load
        vm.saveGameManual()
        val savedMoney = vm.uiState.value.money
        val vm2 = FactoryViewModel(app)
        vm2.continueSavedGame()
        assertEquals(savedMoney, vm2.uiState.value.money)
        assertTrue("TECH_METALWORKING" in vm2.uiState.value.unlockedTechIds)

        // Test Power System overload & offline state transition
        // Remove the starter Biomass Generator at (7, 4) so placed machines lose energy
        vm.demolishAt(7, 4)
        vm.stepSimulation(0.25f)
        val offlineState = vm.uiState.value
        assertEquals(0, offlineState.totalPowerGeneratedKw)
        assertTrue("Machines should be reported as offline", offlineState.offlineMachineCount > 0)
        val offlineExtractor = offlineState.buildings[5 to 6]!!
        assertTrue("Extractor at (5,6) must be offline", offlineExtractor.isOffline)
        assertEquals(OperationalState.OFFLINE, offlineExtractor.operationalState)
    }
}
