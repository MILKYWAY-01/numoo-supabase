package com.example.numoo.activities;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.numoo.R;
import com.example.numoo.adapters.UserDetailAdapter;
import com.example.numoo.supabase.SupabaseAuthHelper;
import com.example.numoo.supabase.SupabaseDbHelper;
import com.example.numoo.models.AppLimit;
import com.example.numoo.models.UsageData;

import java.util.ArrayList;
import java.util.List;

public class UserDetailActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private UserDetailAdapter adapter;
    private SupabaseDbHelper firestoreHelper;
    private TextView tvUserName, tvUsername;
    private String userId, userName, username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_detail);

        try {
            userId = getIntent().getStringExtra("userId");
            userName = getIntent().getStringExtra("userName");
            username = getIntent().getStringExtra("username");

            firestoreHelper = new SupabaseDbHelper(this);

            initViews();
            setupRecyclerView();
            loadData();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void initViews() {
        tvUserName = findViewById(R.id.tv_user_name);
        tvUsername = findViewById(R.id.tv_username);
        recyclerView = findViewById(R.id.recycler_view);
        swipeRefresh = findViewById(R.id.swipe_refresh);

        tvUserName.setText(userName != null ? userName : "User");
        tvUsername.setText(username != null ? "@" + username : "");

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        swipeRefresh.setOnRefreshListener(this::loadData);
    }

    private void setupRecyclerView() {
        adapter = new UserDetailAdapter(this, new ArrayList<>(), new ArrayList<>(),
                new UserDetailAdapter.OnLimitActionListener() {
                    @Override
                    public void onSetLimit(UsageData usageData) {
                        showTimePickerForLimit(usageData);
                    }

                    @Override
                    public void onToggleBlock(UsageData usageData, boolean block) {
                        toggleBlock(usageData, block);
                    }
                });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadData() {
        if (userId == null) return;

        String today = SupabaseDbHelper.getTodayDate();
        firestoreHelper.getUsageDataForDate(userId, today,
                new SupabaseDbHelper.FirestoreCallback<List<UsageData>>() {
                    @Override
                    public void onSuccess(List<UsageData> result) {
                        adapter.updateUsageData(result != null ? result : new ArrayList<>());
                        loadLimits();
                    }

                    @Override
                    public void onError(String error) {
                        swipeRefresh.setRefreshing(false);
                        Toast.makeText(UserDetailActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadLimits() {
        firestoreHelper.getAppLimits(userId,
                new SupabaseDbHelper.FirestoreCallback<List<AppLimit>>() {
                    @Override
                    public void onSuccess(List<AppLimit> result) {
                        adapter.updateLimits(result != null ? result : new ArrayList<>());
                        swipeRefresh.setRefreshing(false);
                    }

                    @Override
                    public void onError(String error) {
                        swipeRefresh.setRefreshing(false);
                    }
                });
    }

    private void showTimePickerForLimit(UsageData usageData) {
        try {
            TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                long limitMillis = (hourOfDay * 60L + minute) * 60 * 1000;
                String adminUid = new SupabaseAuthHelper(this).getCurrentUid();

                AppLimit limit = new AppLimit(
                        usageData.getAppName(),
                        usageData.getPackageName(),
                        limitMillis, false, adminUid
                );

                firestoreHelper.setAppLimit(userId, limit,
                        new SupabaseDbHelper.FirestoreCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                Toast.makeText(UserDetailActivity.this,
                                        "Limit set: " + hourOfDay + "h " + minute + "m",
                                        Toast.LENGTH_SHORT).show();
                                loadData();
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(UserDetailActivity.this,
                                        "Error: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
            }, 1, 0, true);
            dialog.setTitle("Set time limit for " + usageData.getAppName());
            dialog.show();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleBlock(UsageData usageData, boolean block) {
        try {
            String adminUid = new SupabaseAuthHelper(this).getCurrentUid();
            AppLimit limit = new AppLimit(
                    usageData.getAppName(),
                    usageData.getPackageName(),
                    0, block, adminUid
            );

            firestoreHelper.setAppLimit(userId, limit,
                    new SupabaseDbHelper.FirestoreCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Toast.makeText(UserDetailActivity.this,
                                    block ? "App blocked" : "App unblocked",
                                    Toast.LENGTH_SHORT).show();
                            loadData();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(UserDetailActivity.this,
                                    "Error: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}

