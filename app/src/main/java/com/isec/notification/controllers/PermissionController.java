package com.isec.notification.controllers;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import androidx.annotation.NonNull;

import com.isec.notification.services.NotificationMonitorService;

/**
 * Utility class for checking and requesting required permissions.
 */
public final class PermissionController {

    private PermissionController() {}

    /**
     * Returns {@code true} if the Notification Listener permission is currently granted.
     */
    public static boolean hasNotificationListenerPermission(@NonNull Context context) {
        String flat = Settings.Secure.getString(
                context.getContentResolver(),
                "enabled_notification_listeners"
        );
        if (flat == null || flat.isEmpty()) return false;
        String component = new ComponentName(context, NotificationMonitorService.class)
                .flattenToString();
        return flat.contains(component);
    }

    /**
     * Opens the system Notification Listener Settings screen so the user can grant access.
     */
    public static void openNotificationListenerSettings(@NonNull Context context) {
        context.startActivity(
                new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        );
    }
}