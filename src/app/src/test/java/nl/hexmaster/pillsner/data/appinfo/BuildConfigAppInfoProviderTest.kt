package nl.hexmaster.pillsner.data.appinfo

import nl.hexmaster.pillsner.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The About screen's facts are only worth showing if they are the build's own (app-about-screen
 * spec "App identity is read from the build"). What this guards is the mapping: three strings of
 * the same type sit next to each other, and two of them swapped would still compile and still
 * render, just wrongly.
 */
class BuildConfigAppInfoProviderTest {

    @Test
    fun reportsTheApplicationIdTheBuildInstallsAs() {
        assertEquals("nl.hexmaster.pillsner", BuildConfigAppInfoProvider.from("Pillsner").applicationId)
    }

    @Test
    fun reportsTheVersionTheBuildDeclares() {
        val appInfo = BuildConfigAppInfoProvider.from("Pillsner")

        assertEquals(BuildConfig.VERSION_NAME, appInfo.versionName)
        assertEquals(BuildConfig.VERSION_CODE, appInfo.versionCode)
    }

    @Test
    fun theVersionNameIsADottedNumberAndTheCodeCountsUpFromOne() {
        val appInfo = BuildConfigAppInfoProvider.from("Pillsner")

        assertTrue(
            "Version name should read like 0.1.0, was ${appInfo.versionName}",
            appInfo.versionName.matches(Regex("""\d+\.\d+\.\d+""")),
        )
        assertTrue("Version code should be at least 1, was ${appInfo.versionCode}", appInfo.versionCode >= 1)
    }

    @Test
    fun reportsTheNameItIsGiven() {
        assertEquals("Pillsner", BuildConfigAppInfoProvider.from("Pillsner").name)
    }
}
