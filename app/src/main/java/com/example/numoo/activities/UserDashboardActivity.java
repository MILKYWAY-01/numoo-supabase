package com.example.numoo.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.numoo.R;
import com.example.numoo.adapters.AppUsageAdapter;
import com.example.numoo.supabase.SupabaseAuthHelper;
import com.example.numoo.services.UsageTrackingService;
import com.example.numoo.viewmodels.UserDashboardViewModel;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class UserDashboardActivity extends AppCompatActivity {

    private UserDashboardViewModel viewModel;
    private AppUsageAdapter adapter;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private ShimmerFrameLayout shimmerLayout;
    private TextView tvGreeting, tvDate, tvTotalTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);

        try {
            viewModel = new ViewModelProvider(this).get(UserDashboardViewModel.class);
            initViews();
            setupRecyclerView();
            observeViewModel();
            startTrackingService();
            viewModel.loadUsageData();
            viewModel.loadLimits();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void initViews() {
        tvGreeting = findViewById(R.id.tv_greeting);
        tvDate = findViewById(R.id.tv_date);
        tvTotalTime = findViewById(R.id.tv_total_time);
        recyclerView = findViewById(R.id.recycler_view);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        shimmerLayout = findViewById(R.id.shimmer_layout);

        MaterialButton btnLogout = findViewById(R.id.btn_logout);

        String name = viewModel.getUserName();
        tvGreeting.setText("Hello, " + (name != null ? name : "User") + "!");
        tvDate.setText(new SimpleDateFormat("EEEE, MMM dd yyyy", Locale.getDefault()).format(new Date()));

        swipeRefresh.setOnRefreshListener(() -> viewModel.refreshData());

        btnLogout.setOnClickListener(v -> {
            new SupabaseAuthHelper(this).logout();
            startActivity(new Intent(this, RoleSelectionActivity.class));
            finishAffinity();
        });
    }

    private void setupRecyclerView() {
        adapter = new AppUsageAdapter(this, new ArrayList<>(), null);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(this, loading -> {
            if (loading != null) {
                if (loading) {
                    shimmerLayout.setVisibility(View.VISIBLE);
                    shimmerLayout.startShimmer();
                } else {
                    shimmerLayout.stopShimmer();
                    shimmerLayout.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                }
            }
        });

        viewModel.getUsageDataList().observe(this, data -> {
            if (data != null) {
                adapter.updateData(data);
            }
        });

        viewModel.getTotalScreenTime().observe(this, total -> {
            if (total != null) {
                tvTotalTime.setText(formatTime(total));
            }
        });

        viewModel.getAppLimits().observe(this, limits -> {
            if (limits != null) {
                adapter.updateLimits(limits);
            }
        });

        viewModel.getError().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startTrackingService() {
        try {
            Intent serviceIntent = new Intent(this, UsageTrackingService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            viewModel.refreshData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String formatTime(long millis) {
        long hours = millis / (1000 * 60 * 60);
        long minutes = (millis / (1000 * 60)) % 60;
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }
}

