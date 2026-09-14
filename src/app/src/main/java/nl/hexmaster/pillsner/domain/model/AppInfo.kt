package nl.hexmaster.pillsner.domain.model

/**
 * What the app is able to say about itself: the four facts the About screen reports
 * (app-about-screen design D1).
 *
 * A plain value with no Android dependency, so whatever renders it stays unit-testable. It is
 * filled once, in the data layer, from the constants the build generates; nothing in the UI reads
 * a build constant directly, which is what keeps the reported version and the installed package
 * from ever disagreeing.
 *
 * @property name the app's own name, as the user sees it.
 * @property applicationId the package the build installs as, for example `nl.hexmaster.pillsner`.
 * @property versionName the human-readable version, for example `0.1.0`.
 * @property versionCode the monotonically rising build number behind that name.
 */
data class AppInfo(
    val name: String,
    val applicationId: String,
    val versionName: String,
    val versionCode: Int,
)
