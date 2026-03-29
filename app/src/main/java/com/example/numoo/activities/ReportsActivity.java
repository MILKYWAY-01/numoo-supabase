package com.example.numoo.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.numoo.R;
import com.example.numoo.firebase.FirebaseAuthHelper;
import com.example.numoo.firebase.FirestoreHelper;
import com.example.numoo.models.UsageData;
import com.example.numoo.viewmodels.ReportsViewModel;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ReportsActivity extends AppCompatActivity {

    private ReportsViewModel viewModel;
    private BarChart barChart;
    private TextView tvDateRange, tvNoData;
    private MaterialButton btnSelectDate, btnExportCsv;
    private String selectedDate;
    private List<UsageData> currentData = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        try {
            viewModel = new ViewModelProvider(this).get(ReportsViewModel.class);
            selectedDate = FirestoreHelper.getTodayDate();

            initViews();
            observeViewModel();
            loadReport();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void initViews() {
        barChart = findViewById(R.id.bar_chart);
        tvDateRange = findViewById(R.id.tv_date_range);
        tvNoData = findViewById(R.id.tv_no_data);
        btnSelectDate = findViewById(R.id.btn_select_date);
        btnExportCsv = findViewById(R.id.btn_export_csv);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        tvDateRange.setText("Date: " + selectedDate);

        btnSelectDate.setOnClickListener(v -> showDatePicker());
        btnExportCsv.setOnClickListener(v -> exportCsv());

        setupChart();
    }

    private void setupChart() {
        try {
            barChart.getDescription().setEnabled(false);
            barChart.setDrawGridBackground(false);
            barChart.setDrawBarShadow(false);
            barChart.setFitBars(true);
            barChart.animateY(500);
            barChart.setNoDataText("No usage data available");

            XAxis xAxis = barChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setGranularity(1f);
            xAxis.setDrawGridLines(false);
            xAxis.setLabelRotationAngle(-45);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showDatePicker() {
        try {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        Calendar selected = Calendar.getInstance();
                        selected.set(year, month, dayOfMonth);
                        selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                .format(selected.getTime());
                        tvDateRange.setText("Date: " + selectedDate);
                        loadReport();
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadReport() {
        try {
            FirebaseAuthHelper authHelper = new FirebaseAuthHelper(this);
            String uid = authHelper.getCurrentUid();
            if (uid != null) {
                viewModel.loadReportForUser(uid, selectedDate);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void observeViewModel() {
        viewModel.getReportData().observe(this, data -> {
            if (data != null && !data.isEmpty()) {
                currentData = data;
                tvNoData.setVisibility(View.GONE);
                barChart.setVisibility(View.VISIBLE);
                updateChart(data);
            } else {
                currentData = new ArrayList<>();
                tvNoData.setVisibility(View.VISIBLE);
                barChart.setVisibility(View.GONE);
            }
        });

        viewModel.getIsLoading().observe(this, loading -> {
            // Could show progress
        });

        viewModel.getError().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateChart(List<UsageData> data) {
        try {
            // Sort by usage time descending, take top 10
            data.sort((a, b) -> Long.compare(b.getUsageTimeMillis(), a.getUsageTimeMillis()));
            List<UsageData> top = data.subList(0, Math.min(10, data.size()));

            ArrayList<BarEntry> entries = new ArrayList<>();
            ArrayList<String> labels = new ArrayList<>();

            for (int i = 0; i < top.size(); i++) {
                UsageData usage = top.get(i);
                float minutes = usage.getUsageTimeMillis() / (1000f * 60f);
                entries.add(new BarEntry(i, minutes));
                String label = usage.getAppName();
                if (label != null && label.length() > 10) {
                    label = label.substring(0, 10) + "...";
                }
                labels.add(label != null ? label : "Unknown");
            }

            BarDataSet dataSet = new BarDataSet(entries, "Usage (minutes)");
            dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
            dataSet.setValueTextSize(10f);

            BarData barData = new BarData(dataSet);
            barData.setBarWidth(0.7f);

            barChart.setData(barData);
            barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
            barChart.invalidate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void exportCsv() {
        try {
            if (currentData.isEmpty()) {
                Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
                return;
            }

            File dir = new File(getExternalFilesDir(null), "reports");
            if (!dir.exists()) dir.mkdirs();

            String fileName = "numoo_report_" + selectedDate + ".csv";
            File file = new File(dir, fileName);

            FileWriter writer = new FileWriter(file);
            writer.write("App Name,Package Name,Usage (minutes),Date\n");

            for (UsageData data : currentData) {
                float minutes = data.getUsageTimeMillis() / (1000f * 60f);
                writer.write(String.format("\"%s\",\"%s\",%.1f,\"%s\"\n",
                        data.getAppName() != null ? data.getAppName() : "",
                        data.getPackageName() != null ? data.getPackageName() : "",
                        minutes, selectedDate));
            }
            writer.flush();
            writer.close();

            Toast.makeText(this, "Report exported to: " + file.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
