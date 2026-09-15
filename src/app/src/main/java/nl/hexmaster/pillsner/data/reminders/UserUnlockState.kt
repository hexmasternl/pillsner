package nl.hexmaster.pillsner.data.reminders

import android.content.Context
import android.os.UserManager

/**
 * Whether the user has unlocked this device since it booted (design D4).
 *
 * A small interface rather than a direct `UserManager` call, so the wake cycle can be unit-tested
 * without the framework and so the one place that asks is obvious.
 */
fun interface UserUnlockState {

    /** False only between a reboot and the first unlock on a phone with a secure lock screen. */
    fun isUnlocked(): Boolean
}

/**
 * The real answer, from the platform.
 *
 * On a device without file-based encryption or without a secure lock screen this is true always,
 * which is correct: there `BOOT_COMPLETED` arrives immediately and the ordinary path handles it.
 */
class AndroidUserUnlockState(context: Context) : UserUnlockState {

    private val userManager = context.applicationContext.getSystemService(UserManager::class.java)

    override fun isUnlocked(): Boolean = userManager?.isUserUnlocked != false
}
