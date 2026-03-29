package com.example.numoo.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.numoo.R;
import com.example.numoo.models.User;

import java.util.ArrayList;
import java.util.List;

public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.ViewHolder> {

    private final Context context;
    private List<User> userList;
    private final OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public UserListAdapter(Context context, List<User> userList, OnUserClickListener listener) {
        this.context = context;
        this.userList = userList != null ? userList : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            User user = userList.get(position);

            holder.tvName.setText(user.getName() != null ? user.getName() : "User");
            holder.tvUsername.setText(user.getUsername() != null ? "@" + user.getUsername() : "");
            holder.tvEmail.setText(user.getEmail() != null ? user.getEmail() : "");

            // Initial avatar
            String name = user.getName();
            if (name != null && !name.isEmpty()) {
                holder.tvAvatar.setText(String.valueOf(name.charAt(0)).toUpperCase());
            } else {
                holder.tvAvatar.setText("U");
            }

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(user);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return userList != null ? userList.size() : 0;
    }

    public void updateData(List<User> newData) {
        this.userList = newData != null ? newData : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvUsername, tvEmail, tvAvatar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvEmail = itemView.findViewById(R.id.tv_email);
            tvAvatar = itemView.findViewById(R.id.tv_avatar);
        }
    }
}
