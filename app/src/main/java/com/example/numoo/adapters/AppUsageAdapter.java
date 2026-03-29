package com.example.numoo.adapters;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.numoo.R;
import com.example.numoo.models.AppLimit;
import com.example.numoo.models.UsageData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppUsageAdapter extends RecyclerView.Adapter<AppUsageAdapter.ViewHolder> {

    private final Context context;
    private List<UsageData> usageList;
    private Map<String, AppLimit> limitsMap;

    public AppUsageAdapter(Context context, List<UsageData> usageList,
                           List<AppLimit> limits) {
        this.context = context;
        this.usageList = usageList != null ? usageList : new ArrayList<>();
        this.limitsMap = new HashMap<>();
        if (limits != null) {
            for (AppLimit l : limits) {
                limitsMap.put(l.getPackageName(), l);
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_app_usage, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            UsageData item = usageList.get(position);

            holder.tvAppName.setText(item.getAppName() != null ? item.getAppName() : item.getPackageName());
            holder.tvUsageTime.setText(formatTime(item.getUsageTimeMillis()));

            // Load app icon
            try {
                Drawable icon = context.getPackageManager().getApplicationIcon(item.getPackageName());
                holder.ivAppIcon.setImageDrawable(icon);
            } catch (PackageManager.NameNotFoundException e) {
                holder.ivAppIcon.setImageResource(R.drawable.ic_launcher_foreground);
            }

            // Check limit
            AppLimit limit = limitsMap.get(item.getPackageName());
            if (limit != null) {
                long limitMillis = limit.getLimitMillis();
                if (limit.isBlocked()) {
                    holder.tvStatus.setText("BLOCKED");
                    holder.tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
                    holder.progressBar.setProgress(100);
                    holder.tvLimit.setText("Blocked by admin");
                } else if (limitMillis > 0) {
                    int progress = (int) ((item.getUsageTimeMillis() * 100) / limitMillis);
                    holder.progressBar.setProgress(Math.min(progress, 100));
                    holder.tvLimit.setText("Limit: " + formatTime(limitMillis));

                    if (item.getUsageTimeMillis() >= limitMillis) {
                        holder.tvStatus.setText("LIMITED");
                        holder.tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_orange_dark));
                    } else {
                        holder.tvStatus.setText("ACTIVE");
                        holder.tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
                    }
                } else {
                    setNoLimit(holder);
                }
            } else {
                setNoLimit(holder);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setNoLimit(ViewHolder holder) {
        holder.tvStatus.setText("ACTIVE");
        holder.tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
        holder.progressBar.setProgress(0);
        holder.tvLimit.setText("No limit set");
    }

    @Override
    public int getItemCount() {
        return usageList != null ? usageList.size() : 0;
    }

    public void updateData(List<UsageData> newData) {
        this.usageList = newData != null ? newData : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void updateLimits(List<AppLimit> limits) {
        this.limitsMap.clear();
        if (limits != null) {
            for (AppLimit l : limits) {
                limitsMap.put(l.getPackageName(), l);
            }
        }
        notifyDataSetChanged();
    }

    private String formatTime(long millis) {
        long hours = millis / (1000 * 60 * 60);
        long minutes = (millis / (1000 * 60)) % 60;
        if (hours > 0) return hours + "h " + minutes + "m";
        return minutes + "m";
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAppIcon;
        TextView tvAppName, tvUsageTime, tvLimit, tvStatus;
        ProgressBar progressBar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAppIcon = itemView.findViewById(R.id.iv_app_icon);
            tvAppName = itemView.findViewById(R.id.tv_app_name);
            tvUsageTime = itemView.findViewById(R.id.tv_usage_time);
            tvLimit = itemView.findViewById(R.id.tv_limit);
            tvStatus = itemView.findViewById(R.id.tv_status);
            progressBar = itemView.findViewById(R.id.progress_bar);
        }
    }
}
