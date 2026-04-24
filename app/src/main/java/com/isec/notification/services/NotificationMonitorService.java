package com.isec.notification.services;

import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import androidx.annotation.NonNull;

import com.isec.notification.monitors.NotificationMonitor;
import com.isec.notification.models.NotificationModel;

import java.util.ArrayList;

/**
 * System-bound service that listens for all posted notifications.
 * Forwards received notifications to {@link NotificationMonitor} via a local broadcast.
 */
public class NotificationMonitorService extends NotificationListenerService {

    /** Package name filter — empty means accept all packages */
    @NonNull
    private static volatile String filterPackageName = "";

    public static void setFilterPackageName(@NonNull String packageName) {
        filterPackageName = packageName;
    }

    @NonNull
    public static String getFilterPackageName() {
        return filterPackageName;
    }

    // -------------------------------------------------------------------------
    // NotificationListenerService callbacks
    // -------------------------------------------------------------------------

    @Override
    public void onNotificationPosted(@NonNull StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);

        String sbnPackage = sbn.getPackageName();
        Notification notification = sbn.getNotification();

        if (notification == null) return;
        if (!filterPackageName.isEmpty() && !filterPackageName.equals(sbnPackage)) return;

        // Build our model from the status-bar notification
        NotificationModel model = buildModel(sbn, notification);

        // Dispatch to the in-process monitor via local broadcast
        Intent intent = new Intent(NotificationMonitor.ACTION_NOTIFICATION_POSTED);
        intent.putExtra(NotificationMonitor.EXTRA_NOTIFICATION_MODEL, model);
        sendBroadcast(intent);
    }

    @Override
    public void onNotificationRemoved(@NonNull StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        Intent intent = new Intent(NotificationMonitor.ACTION_NOTIFICATION_REMOVED);
        intent.putExtra(NotificationMonitor.EXTRA_PACKAGE_NAME, sbn.getPackageName());
        sendBroadcast(intent);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    @NonNull
    private NotificationModel buildModel(@NonNull StatusBarNotification sbn,
                                         @NonNull Notification notification) {
        NotificationModel model = new NotificationModel();
        model.setId(sbn.getId());
        model.setPackageName(sbn.getPackageName());
        model.setPostTime(sbn.getPostTime());
        model.setOngoing(sbn.isOngoing());
        model.setChannelId(notification.getChannelId());
        model.setPriority(notification.priority);

        Bundle extras = notification.extras;
        if (extras == null) return model;

        // ── 1. Basic text fields ─────────────────────────────────────────────
        model.setTitle(safeString(extras.getCharSequence(Notification.EXTRA_TITLE)));
        model.setText(safeString(extras.getCharSequence(Notification.EXTRA_TEXT)));
        model.setSubText(safeString(extras.getCharSequence(Notification.EXTRA_SUB_TEXT)));
        model.setBigText(safeString(extras.getCharSequence(Notification.EXTRA_BIG_TEXT)));
        model.setInfoText(safeString(extras.getCharSequence(Notification.EXTRA_INFO_TEXT)));

        // ── 2. MessagingStyle — EXTRA_MESSAGES is a Parcelable[] of Bundles ─
        //    Each Bundle contains:
        //      "text"   (CharSequence) — the message body
        //      "sender" (CharSequence) — sender name (may be null for self)
        //      "time"   (long)         — epoch millis
        android.os.Parcelable[] msgArray =
                extras.getParcelableArray(Notification.EXTRA_MESSAGES);
        if (msgArray != null && msgArray.length > 0) {
            ArrayList<String> messages = new ArrayList<>();
            for (android.os.Parcelable p : msgArray) {
                if (p instanceof Bundle) {
                    Bundle b = (Bundle) p;
                    CharSequence sender  = b.getCharSequence("sender");
                    CharSequence msgText = b.getCharSequence("text");
                    if (msgText != null && !msgText.toString().isEmpty()) {
                        String line = (sender != null && !sender.toString().isEmpty())
                                ? sender + ": " + msgText
                                : msgText.toString();
                        messages.add(line);
                    }
                }
            }
            model.setMessages(messages);
        }

        // ── 3. InboxStyle — EXTRA_TEXT_LINES is a CharSequence[] ────────────
        CharSequence[] lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if (lines != null && lines.length > 0) {
            ArrayList<String> textLines = new ArrayList<>();
            for (CharSequence line : lines) {
                if (line != null && !line.toString().isEmpty()) {
                    textLines.add(line.toString());
                }
            }
            model.setTextLines(textLines);
        }

        // ── 4. Raw extras — serialize every key to a human-readable string ──
        ArrayList<String> rawList = new ArrayList<>();
        for (String key : extras.keySet()) {
            rawList.add(key + " = " + serializeExtra(extras.get(key)));
        }
        model.setRawExtras(rawList);

        return model;
    }

    /**
     * Converts any extra value to a readable string.
     * Arrays and Parcelable arrays are expanded instead of printing a memory address.
     */
    @NonNull
    private String serializeExtra(Object value) {
        if (value == null) return "null";

        // CharSequence[] (e.g. EXTRA_TEXT_LINES)
        if (value instanceof CharSequence[]) {
            CharSequence[] arr = (CharSequence[]) value;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < arr.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(arr[i] != null ? arr[i].toString() : "null");
            }
            return sb.append("]").toString();
        }

        // Parcelable[] (e.g. EXTRA_MESSAGES)
        if (value instanceof android.os.Parcelable[]) {
            android.os.Parcelable[] arr = (android.os.Parcelable[]) value;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < arr.length; i++) {
                if (i > 0) sb.append(" | ");
                if (arr[i] instanceof Bundle) {
                    Bundle b = (Bundle) arr[i];
                    // Flatten the inner bundle key=value pairs
                    StringBuilder inner = new StringBuilder("{");
                    boolean first = true;
                    for (String k : b.keySet()) {
                        if (!first) inner.append(", ");
                        inner.append(k).append("=").append(serializeExtra(b.get(k)));
                        first = false;
                    }
                    sb.append(inner).append("}");
                } else {
                    sb.append(arr[i] != null ? arr[i].toString() : "null");
                }
            }
            return sb.append("]").toString();
        }

        // int[] / long[] / String[] primitives
        if (value instanceof int[])    return java.util.Arrays.toString((int[]) value);
        if (value instanceof long[])   return java.util.Arrays.toString((long[]) value);
        if (value instanceof float[])  return java.util.Arrays.toString((float[]) value);
        if (value instanceof boolean[])return java.util.Arrays.toString((boolean[]) value);
        if (value instanceof String[]) return java.util.Arrays.toString((String[]) value);

        return value.toString();
    }

    @NonNull
    private String safeString(CharSequence cs) {
        return cs != null ? cs.toString() : "";
    }
}
