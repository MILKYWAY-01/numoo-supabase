package com.example.numoo.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.numoo.supabase.SupabaseDbHelper;
import com.example.numoo.models.UsageData;

import java.util.ArrayList;
import java.util.List;

public class ReportsViewModel extends AndroidViewModel {

    private final MutableLiveData<List<UsageData>> reportData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private final SupabaseDbHelper firestoreHelper;

    public ReportsViewModel(@NonNull Application application) {
        super(application);
        firestoreHelper = new SupabaseDbHelper(application);
    }

    public LiveData<List<UsageData>> getReportData() { return reportData; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getError() { return error; }

    public void loadReportForUser(String userId, String date) {
        isLoading.setValue(true);
        firestoreHelper.getUsageDataForDate(userId, date,
            new SupabaseDbHelper.FirestoreCallback<List<UsageData>>() {
                @Override
                public void onSuccess(List<UsageData> result) {
                    reportData.postValue(result != null ? result : new ArrayList<>());
                    isLoading.postValue(false);
                }

                @Override
                public void onError(String err) {
                    error.postValue(err);
                    isLoading.postValue(false);
                }
            });
    }
}

