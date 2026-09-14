package nl.hexmaster.pillsner.data.appinfo

import android.content.Context
import nl.hexmaster.pillsner.BuildConfig
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.domain.model.AppInfo

/**
 * Reads the app's identity out of the build (app-about-screen design D1, D2).
 *
 * The application id, the version name and the version code come from the constants Gradle
 * generates, so raising a version in `build.gradle.kts` is enough for the About screen to report
 * it; no Kotlin source states them a second time and no runtime package lookup can fail. The name
 * is the `app_name` resource, which is what the launcher shows.
 */
object BuildConfigAppInfoProvider {

    /** The identity of this build, with the app name read from [context]'s resources. */
    fun provide(context: Context): AppInfo = from(context.getString(R.string.app_name))

    /**
     * The identity of this build under a given [name]. Split out from [provide] so the mapping of
     * build constants onto fields is unit-testable without a `Context`.
     */
    internal fun from(name: String): AppInfo = AppInfo(
        name = name,
        applicationId = BuildConfig.APPLICATION_ID,
        versionName = BuildConfig.VERSION_NAME,
        versionCode = BuildConfig.VERSION_CODE,
    )
}
