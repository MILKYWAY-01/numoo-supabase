package com.example.numoo.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
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
import com.example.numoo.adapters.UserListAdapter;
import com.example.numoo.viewmodels.AdminDashboardViewModel;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

public class AdminDashboardActivity extends AppCompatActivity {

    private AdminDashboardViewModel viewModel;
    private UserListAdapter adapter;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private ShimmerFrameLayout shimmerLayout;
    private TextView tvGreeting, tvAdminCode, tvUserCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        try {
            viewModel = new ViewModelProvider(this).get(AdminDashboardViewModel.class);
            initViews();
            setupRecyclerView();
            observeViewModel();
            viewModel.loadLinkedUsers();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void initViews() {
        tvGreeting = findViewById(R.id.tv_greeting);
        tvAdminCode = findViewById(R.id.tv_admin_code);
        tvUserCount = findViewById(R.id.tv_user_count);
        recyclerView = findViewById(R.id.recycler_view);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        shimmerLayout = findViewById(R.id.shimmer_layout);

        MaterialButton btnLogout = findViewById(R.id.btn_logout);
        MaterialButton btnReports = findViewById(R.id.btn_reports);
        MaterialButton btnCopyCode = findViewById(R.id.btn_copy_code);

        String name = viewModel.getAdminName();
        tvGreeting.setText("Welcome, " + (name != null ? name : "Admin") + "!");

        swipeRefresh.setOnRefreshListener(() -> viewModel.loadLinkedUsers());

        btnLogout.setOnClickListener(v -> {
            viewModel.logout();
            startActivity(new Intent(this, RoleSelectionActivity.class));
            finishAffinity();
        });

        btnReports.setOnClickListener(v -> {
            startActivity(new Intent(this, ReportsActivity.class));
        });

        btnCopyCode.setOnClickListener(v -> {
            String code = tvAdminCode.getText().toString();
            if (!code.isEmpty() && !"Loading...".equals(code)) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("Admin Code", code));
                    Toast.makeText(this, "Code copied!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new UserListAdapter(this, new ArrayList<>(), user -> {
            Intent intent = new Intent(this, UserDetailActivity.class);
            intent.putExtra("userId", user.getUid());
            intent.putExtra("userName", user.getName());
            intent.putExtra("username", user.getUsername());
            startActivity(intent);
        });
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

        viewModel.getLinkedUsers().observe(this, users -> {
            if (users != null) {
                adapter.updateData(users);
                tvUserCount.setText(users.size() + " user" + (users.size() != 1 ? "s" : "") + " linked");
            }
        });

        viewModel.getAdminCode().observe(this, code -> {
            if (code != null) {
                tvAdminCode.setText(code);
            }
        });

        viewModel.getError().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            viewModel.loadLinkedUsers();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
