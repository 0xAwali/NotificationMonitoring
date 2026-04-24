package com.isec.notification.monitors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.isec.notification.models.NotificationModel;

import java.util.function.Consumer;

/**
 * Singleton BroadcastReceiver that bridges the {@link com.isec.notification.services.NotificationMonitorService}
 * with the UI layer.
 *
 * <p>Register it once (call {@link #start}) and unregister when done (call {@link #stop}).
 * Set a callback via {@link #setOnNotificationPosted} to receive every captured notification.</p>
 */
public class NotificationMonitor extends BroadcastReceiver {

    // -------------------------------------------------------------------------
    // Public constants (shared with NotificationMonitorService)
    // -------------------------------------------------------------------------

    public static final String ACTION_NOTIFICATION_POSTED  = "com.isec.notification.ACTION_POSTED";
    public static final String ACTION_NOTIFICATION_REMOVED = "com.isec.notification.ACTION_REMOVED";

    public static final String EXTRA_NOTIFICATION_MODEL    = "extra_notification_model";
    public static final String EXTRA_PACKAGE_NAME          = "extra_package_name";

    // -------------------------------------------------------------------------
    // Singleton
    // -------------------------------------------------------------------------

    @Nullable
    private static NotificationMonitor instance;

    private NotificationMonitor() {}

    @NonNull
    public static synchronized NotificationMonitor getInstance() {
        if (instance == null) {
            instance = new NotificationMonitor();
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private volatile boolean listening = false;

    @Nullable private Consumer<NotificationModel> onPosted;
    @Nullable private Consumer<String>            onRemoved;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public void setOnNotificationPosted(@Nullable Consumer<NotificationModel> callback) {
        this.onPosted = callback;
    }

    public void setOnNotificationRemoved(@Nullable Consumer<String> callback) {
        this.onRemoved = callback;
    }

    public boolean isListening() {
        return listening;
    }

    /**
     * Register the receiver and start accepting broadcast events.
     */
    public void start(@NonNull Context context) {
        if (listening) return;
        listening = true;

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_NOTIFICATION_POSTED);
        filter.addAction(ACTION_NOTIFICATION_REMOVED);

        ContextCompat.registerReceiver(
                context.getApplicationContext(),
                this,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
        );
    }

    /**
     * Unregister the receiver and stop accepting events.
     */
    public void stop(@NonNull Context context) {
        if (!listening) return;
        listening = false;
        try {
            context.getApplicationContext().unregisterReceiver(this);
        } catch (IllegalArgumentException ignored) {
            // receiver was not registered — safe to ignore
        }
    }

    // -------------------------------------------------------------------------
    // BroadcastReceiver
    // -------------------------------------------------------------------------

    @Override
    public void onReceive(@NonNull Context context, @Nullable Intent intent) {
        if (intent == null) return;

        switch (intent.getAction() != null ? intent.getAction() : "") {
            case ACTION_NOTIFICATION_POSTED: {
                NotificationModel model = intent.getParcelableExtra(EXTRA_NOTIFICATION_MODEL);
                if (model != null && onPosted != null) {
                    onPosted.accept(model);
                }
                break;
            }
            case ACTION_NOTIFICATION_REMOVED: {
                String pkg = intent.getStringExtra(EXTRA_PACKAGE_NAME);
                if (pkg != null && onRemoved != null) {
                    onRemoved.accept(pkg);
                }
                break;
            }
        }
    }
}