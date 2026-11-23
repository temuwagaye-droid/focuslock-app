package com.example.security

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import com.example.service.ReadLockAccessibilityService

object AccessibilityHelper {

    /**
     * Deep-links directly to Android Accessibility Settings focused on FocusLock's
     * ReadLockAccessibilityService instead of dropping the user into generic app details.
     */
    fun openAccessibilitySettings(context: Context) {
        val targetService = ComponentName(context, ReadLockAccessibilityService::class.java).flattenToString()

        // Attempt 1: Deep link directly into Accessibility Settings pointing to ReadLockAccessibilityService
        val directIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(":settings:fragment_args_key", targetService)
            val args = Bundle().apply {
                putString(":settings:fragment_args_key", targetService)
            }
            putExtra(":settings:show_fragment_args", args)
        }

        try {
            context.startActivity(directIntent)
            return
        } catch (_: Exception) {}

        // Attempt 2: General Accessibility Settings
        try {
            val generalIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(generalIntent)
            return
        } catch (_: Exception) {}

        // Attempt 3: App Details Settings fallback
        try {
            val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
        } catch (_: Exception) {}
    }
}
