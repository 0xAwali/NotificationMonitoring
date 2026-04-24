package com.isec.notification.activities;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.isec.notification.R;
import com.isec.notification.controllers.NotificationAdapter;
import com.isec.notification.controllers.PermissionController;
import com.isec.notification.models.NotificationModel;
import com.isec.notification.monitors.NotificationMonitor;
import com.isec.notification.services.NotificationMonitorService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // -------------------------------------------------------------------------
    // Views
    // -------------------------------------------------------------------------
    private TextView    tvStatus;
    private TextView    tvPermissionBanner;
    private Button      btnToggle;
    private Button      btnClear;
    private Button      btnGrantPermission;
    private EditText    etPackageFilter;
    private RecyclerView recyclerView;
    private TextView    tvEmpty;
    private TextView    tvCaptureCount;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------
    private NotificationAdapter adapter;
    private LinearLayoutManager layoutManager;
    private final NotificationMonitor monitor = NotificationMonitor.getInstance();
    private int captureCount = 0;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        setupRecyclerView();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPermissionState();
    }

    @Override
    protected void onDestroy() {
        stopMonitoring();
        super.onDestroy();
    }

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    private void bindViews() {
        tvStatus           = findViewById(R.id.tv_status);
        tvPermissionBanner = findViewById(R.id.tv_permission_banner);
        btnToggle          = findViewById(R.id.btn_toggle);
        btnClear           = findViewById(R.id.btn_clear);
        btnGrantPermission = findViewById(R.id.btn_grant_permission);
        etPackageFilter    = findViewById(R.id.et_package_filter);
        recyclerView       = findViewById(R.id.recycler_view);
        tvEmpty            = findViewById(R.id.tv_empty);
        tvCaptureCount     = findViewById(R.id.tv_capture_count);
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(this::showDetailDialog);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(false);
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        btnGrantPermission.setOnClickListener(v ->
                PermissionController.openNotificationListenerSettings(this));

        btnToggle.setOnClickListener(v -> {
            if (monitor.isListening()) {
                stopMonitoring();
            } else {
                startMonitoring();
            }
        });

        btnClear.setOnClickListener(v -> {
            adapter.clearAll();
            captureCount = 0;
            updateCaptureCount();
            updateEmptyState();
        });

        etPackageFilter.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                NotificationMonitorService.setFilterPackageName(s.toString().trim());
            }
        });
    }

    // -------------------------------------------------------------------------
    // Monitoring control
    // -------------------------------------------------------------------------

    private void startMonitoring() {
        if (!PermissionController.hasNotificationListenerPermission(this)) {
            Toast.makeText(this, R.string.toast_permission_required, Toast.LENGTH_LONG).show();
            PermissionController.openNotificationListenerSettings(this);
            return;
        }

        NotificationMonitorService.setFilterPackageName(
                etPackageFilter.getText().toString().trim());

        monitor.setOnNotificationPosted(model -> runOnUiThread(() -> {
            adapter.addItem(model);
            captureCount++;
            updateCaptureCount();
            updateEmptyState();
            // Auto-scroll to top only when user is already at or near the top
            // (within 3 items), so reading further down is not interrupted
            if (layoutManager.findFirstVisibleItemPosition() <= 2) {
                recyclerView.scrollToPosition(0);
            }
        }));

        monitor.setOnNotificationRemoved(pkg -> runOnUiThread(() ->
                Toast.makeText(this,
                        getString(R.string.toast_notification_removed, pkg),
                        Toast.LENGTH_SHORT).show()
        ));

        monitor.start(this);

        updateStatus(true);
        btnToggle.setText(R.string.btn_stop);
    }

    private void stopMonitoring() {
        monitor.stop(this);
        updateStatus(false);
        btnToggle.setText(R.string.btn_start);
    }

    // -------------------------------------------------------------------------
    // UI helpers
    // -------------------------------------------------------------------------

    private void refreshPermissionState() {
        boolean hasPermission = PermissionController.hasNotificationListenerPermission(this);
        tvPermissionBanner.setVisibility(hasPermission ? View.GONE : View.VISIBLE);
        btnGrantPermission.setVisibility(hasPermission ? View.GONE : View.VISIBLE);
        btnToggle.setEnabled(hasPermission);

        if (!hasPermission && monitor.isListening()) {
            stopMonitoring();
        }
    }

    private void updateStatus(boolean active) {
        tvStatus.setText(active ? R.string.status_active : R.string.status_idle);
        tvStatus.setBackgroundResource(active
                ? R.drawable.bg_status_active
                : R.drawable.bg_status_idle);
    }

    private void updateEmptyState() {
        boolean empty = adapter.getItemCount() == 0;
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void updateCaptureCount() {
        tvCaptureCount.setText(getString(R.string.label_total_captured, captureCount));
    }

    // -------------------------------------------------------------------------
    // Detail dialog
    // -------------------------------------------------------------------------

    private void showDetailDialog(@NonNull NotificationModel model) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        StringBuilder sb = new StringBuilder();

        // ── Meta ──────────────────────────────────────────────────────────────
        appendField(sb, "Package",  model.getPackageName());
        appendField(sb, "Channel",  model.getChannelId());
        appendField(sb, "Time",     sdf.format(new Date(model.getPostTime())));
        appendField(sb, "Ongoing",  String.valueOf(model.isOngoing()));
        appendField(sb, "Priority", String.valueOf(model.getPriority()));

        // ── Basic text ────────────────────────────────────────────────────────
        sb.append("\n");
        appendField(sb, "Title", model.getTitle());

        if (!model.getSubText().isEmpty())
            appendField(sb, "SubText", model.getSubText());
        if (!model.getInfoText().isEmpty())
            appendField(sb, "InfoText", model.getInfoText());

        // ── MessagingStyle messages (WhatsApp, Telegram, SMS…) ───────────────
        if (!model.getMessages().isEmpty()) {
            sb.append("\n──── Messages ────\n");
            for (String msg : model.getMessages()) {
                sb.append("• ").append(msg).append("\n");
            }
        } else if (!model.getBigText().isEmpty()) {
            // BigTextStyle
            sb.append("\n──── Big Text ────\n");
            sb.append(model.getBigText()).append("\n");
        } else if (!model.getTextLines().isEmpty()) {
            // InboxStyle (Gmail, email clients…)
            sb.append("\n──── Inbox Lines ────\n");
            for (String line : model.getTextLines()) {
                sb.append("• ").append(line).append("\n");
            }
        } else if (!model.getText().isEmpty()) {
            sb.append("\n──── Text ────\n");
            sb.append(model.getText()).append("\n");
        }

        // ── Raw extras ────────────────────────────────────────────────────────
        if (!model.getRawExtras().isEmpty()) {
            sb.append("\n──── Raw Extras ────\n");
            for (String extra : model.getRawExtras()) {
                sb.append("• ").append(extra).append("\n");
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_detail_title)
                .setMessage(sb.toString().trim())
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void appendField(@NonNull StringBuilder sb,
                             @NonNull String label,
                             @NonNull String value) {
        sb.append(label).append(":  ")
                .append(value.isEmpty() ? "—" : value)
                .append("\n");
    }
}
