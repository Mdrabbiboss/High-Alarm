package com.programmer.highalarm

import android.content.Context
import java.io.File

/**
 * Makes a fresh install really start fresh.
 *
 * Some phones (Samsung Smart Switch, Google backup, device-to-device transfer) can
 * bring back an old copy of the app's settings when it is reinstalled. Then the
 * onboarding was skipped, even though none of the permissions it asks for were
 * granted any more, and stale alarm data came back without being scheduled.
 *
 * We remember which installation wrote the data. If the data belongs to a different
 * installation, it was restored from somewhere else, so it is cleared and the app
 * starts from the first onboarding step. Normal app updates keep everything.
 */
object FreshInstallGuard {
    private const val GUARD_PREFS = "install_guard"
    private const val KEY_STAMP = "install_stamp"

    fun check(context: Context) {
        val info = try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (_: Exception) {
            return
        }
        val installTime = info.firstInstallTime
        val guard = context.getSharedPreferences(GUARD_PREFS, Context.MODE_PRIVATE)
        val stamp = guard.getLong(KEY_STAMP, -1L)

        val restoredFromElsewhere = when {
            stamp == installTime -> false
            stamp != -1L -> true
            else -> {
                // First run of a build that has this guard. On a plain update keep the
                // data; on a brand-new install, finished onboarding means it was restored.
                val freshInstall = info.firstInstallTime == info.lastUpdateTime
                val onboarded = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                    .getBoolean("is_onboarding_completed", false)
                freshInstall && onboarded
            }
        }

        if (restoredFromElsewhere) clearRestoredData(context)
        if (stamp != installTime) guard.edit().putLong(KEY_STAMP, installTime).commit()
    }

    private fun clearRestoredData(context: Context) {
        val dir = File(context.applicationInfo.dataDir, "shared_prefs")
        dir.listFiles()
            ?.filter { it.name.endsWith(".xml") }
            ?.map { it.name.removeSuffix(".xml") }
            ?.filter { it != GUARD_PREFS }
            ?.forEach { name ->
                context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
            }
    }
}
