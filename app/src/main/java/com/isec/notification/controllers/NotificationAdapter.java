package com.isec.notification.controllers;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.isec.notification.R;
import com.isec.notification.models.NotificationModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter that displays a list of captured {@link NotificationModel} entries.
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(@NonNull NotificationModel model);
    }

    @NonNull private final List<NotificationModel> items = new ArrayList<>();
    @NonNull private final SimpleDateFormat sdf =
            new SimpleDateFormat("HH:mm:ss  dd/MM/yyyy", Locale.getDefault());

    @NonNull private OnItemClickListener listener;

    public NotificationAdapter(@NonNull OnItemClickListener listener) {
        this.listener = listener;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Insert or update a notification.
     * <p>
     * Android fires {@code onNotificationPosted} every time a notification is <em>updated</em>
     * (e.g. a message-count badge changes), not only when it first appears. Without
     * deduplication this would keep inserting identical rows.
     * <p>
     * Strategy: a notification is uniquely identified by its numeric {@code id} AND its
     * {@code packageName}. If a matching entry already exists, update it in place; otherwise
     * insert at position 0 (newest first).
     */
    public void addItem(@NonNull NotificationModel model) {
        // Search for an existing entry with the same id + package
        for (int i = 0; i < items.size(); i++) {
            NotificationModel existing = items.get(i);
            if (existing.getId() == model.getId()
                    && existing.getPackageName().equals(model.getPackageName())) {
                // Update in place — move to top so it stays "newest first"
                items.remove(i);
                items.add(0, model);
                notifyItemMoved(i, 0);
                notifyItemChanged(0);
                return;
            }
        }
        // Brand-new notification — insert at top
        items.add(0, model);
        notifyItemInserted(0);
    }

    public void clearAll() {
        int size = items.size();
        items.clear();
        notifyItemRangeRemoved(0, size);
    }

    @NonNull
    public List<NotificationModel> getItems() {
        return items;
    }

    // -------------------------------------------------------------------------
    // RecyclerView.Adapter
    // -------------------------------------------------------------------------

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationModel model = items.get(position);

        holder.tvPackage.setText(model.getPackageName());
        holder.tvTitle.setText(model.getTitle().isEmpty() ? "(no title)" : model.getTitle());

        // Show the richest available content in the preview row:
        // MessagingStyle messages > BigText > InboxStyle lines > plain text
        String preview;
        if (!model.getMessages().isEmpty()) {
            // Join last 2 messages so the row isn't too tall
            List<String> msgs = model.getMessages();
            int from = Math.max(0, msgs.size() - 2);
            preview = String.join("\n", msgs.subList(from, msgs.size()));
        } else if (!model.getBigText().isEmpty()) {
            preview = model.getBigText();
        } else if (!model.getTextLines().isEmpty()) {
            preview = String.join("\n", model.getTextLines());
        } else {
            preview = model.getText();
        }
        holder.tvText.setText(preview.isEmpty() ? "(no content)" : preview);

        holder.tvTime.setText(sdf.format(new Date(model.getPostTime())));
        holder.tvOngoing.setVisibility(model.isOngoing() ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> listener.onItemClick(model));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // -------------------------------------------------------------------------
    // ViewHolder
    // -------------------------------------------------------------------------

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvPackage;
        final TextView tvTitle;
        final TextView tvText;
        final TextView tvTime;
        final TextView tvOngoing;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPackage = itemView.findViewById(R.id.tv_package);
            tvTitle   = itemView.findViewById(R.id.tv_title);
            tvText    = itemView.findViewById(R.id.tv_text);
            tvTime    = itemView.findViewById(R.id.tv_time);
            tvOngoing = itemView.findViewById(R.id.tv_ongoing);
        }
    }
}
