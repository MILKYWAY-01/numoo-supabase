package com.example.numoo.adapters;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.numoo.R;
import com.example.numoo.models.AppLimit;
import com.example.numoo.models.UsageData;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserDetailAdapter extends RecyclerView.Adapter<UserDetailAdapter.ViewHolder> {

    private final Context context;
    private List<UsageData> usageList;
    private Map<String, AppLimit> limitsMap;
    private final OnLimitActionListener listener;

    public interface OnLimitActionListener {
        void onSetLimit(UsageData usageData);
        void onToggleBlock(UsageData usageData, boolean block);
    }

    public UserDetailAdapter(Context context, List<UsageData> usageList,
                             List<AppLimit> limits, OnLimitActionListener listener) {
        this.context = context;
        this.usageList = usageList != null ? usageList : new ArrayList<>();
        this.limitsMap = new HashMap<>();
        this.listener = listener;
        if (limits != null) {
            for (AppLimit l : limits) {
                limitsMap.put(l.getPackageName(), l);
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_detail_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            UsageData item = usageList.get(position);

            holder.tvAppName.setText(item.getAppName() != null ? item.getAppName() : item.getPackageName());
            holder.tvUsageTime.setText("Used: " + formatTime(item.getUsageTimeMillis()));

            // App icon
            try {
                if (item.getPackageName() != null) {
                    Drawable icon = context.getPackageManager().getApplicationIcon(item.getPackageName());
                    holder.ivAppIcon.setImageDrawable(icon);
                }
            } catch (PackageManager.NameNotFoundException e) {
                holder.ivAppIcon.setImageResource(R.drawable.ic_launcher_foreground);
            }

            // Limit info
            AppLimit limit = limitsMap.get(item.getPackageName());
            if (limit != null) {
                if (limit.getLimitMillis() > 0) {
                    holder.tvLimitInfo.setText("Limit: " + formatTime(limit.getLimitMillis()));
                } else {
                    holder.tvLimitInfo.setText("No time limit");
                }
                holder.switchBlock.setOnCheckedChangeListener(null);
                holder.switchBlock.setChecked(limit.isBlocked());
            } else {
                holder.tvLimitInfo.setText("No limit set");
                holder.switchBlock.setOnCheckedChangeListener(null);
                holder.switchBlock.setChecked(false);
            }

            holder.btnSetLimit.setOnClickListener(v -> {
                if (listener != null) listener.onSetLimit(item);
            });

            holder.switchBlock.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) listener.onToggleBlock(item, isChecked);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return usageList != null ? usageList.size() : 0;
    }

    public void updateUsageData(List<UsageData> newData) {
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
        TextView tvAppName, tvUsageTime, tvLimitInfo;
        MaterialButton btnSetLimit;
        SwitchMaterial switchBlock;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAppIcon = itemView.findViewById(R.id.iv_app_icon);
            tvAppName = itemView.findViewById(R.id.tv_app_name);
            tvUsageTime = itemView.findViewById(R.id.tv_usage_time);
            tvLimitInfo = itemView.findViewById(R.id.tv_limit_info);
            btnSetLimit = itemView.findViewById(R.id.btn_set_limit);
            switchBlock = itemView.findViewById(R.id.switch_block);
        }
    }
}
