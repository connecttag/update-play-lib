package org.connecttag.lib.update

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.connecttag.lib.update.fake.FakeAppUpdateEngine
import org.connecttag.lib.update.model.AppUpdateStatus
import org.connecttag.lib.update.model.AppUpdateType
import org.connecttag.lib.update.model.UpdateAvailabilityResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FakeAppUpdateEngineTest {

    private lateinit var updateEngine: FakeAppUpdateEngine

    @Before
    fun setUp() {
        updateEngine = FakeAppUpdateEngine()
    }

    @Test
    fun initialState_isIdle() {
        assertEquals(AppUpdateStatus.Idle, updateEngine.updateStatus.value)
    }

    @Test
    fun checkForUpdate_whenAvailable_updatesStatusToAvailable() = runTest {
        updateEngine.isUpdateAvailable = true
        updateEngine.availableVersionCode = 205

        val result = updateEngine.checkForUpdate()

        assertTrue(result is UpdateAvailabilityResult.Available)
        val available = result as UpdateAvailabilityResult.Available
        assertEquals(205, available.availableVersionCode)
        assertTrue(updateEngine.updateStatus.value is AppUpdateStatus.Available)
    }

    @Test
    fun checkForUpdate_whenNotAvailable_updatesStatusToNotAvailable() = runTest {
        updateEngine.isUpdateAvailable = false

        val result = updateEngine.checkForUpdate()

        assertEquals(UpdateAvailabilityResult.NotAvailable, result)
        assertEquals(AppUpdateStatus.NotAvailable, updateEngine.updateStatus.value)
    }

    @Test
    fun flexibleUpdate_simulationFlow_transitionsThroughStates() = runTest {
        updateEngine.checkForUpdate()

        val launched = updateEngine.startUpdateFlow(
            activity = mockActivity(),
            updateType = AppUpdateType.FLEXIBLE
        )
        assertTrue(launched)
        assertTrue(updateEngine.updateStatus.value is AppUpdateStatus.Downloading)

        updateEngine.simulateDownloaded()
        assertEquals(AppUpdateStatus.Downloaded, updateEngine.updateStatus.value)

        updateEngine.completeUpdate()
        assertEquals(AppUpdateStatus.Installed, updateEngine.updateStatus.value)
    }

    private fun mockActivity(): android.app.Activity {
        val field = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").apply { isAccessible = true }
        val unsafe = field.get(null) as sun.misc.Unsafe
        return unsafe.allocateInstance(android.app.Activity::class.java) as android.app.Activity
    }
}
