package com.example.numoo.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.numoo.firebase.FirebaseAuthHelper;
import com.example.numoo.firebase.FirestoreHelper;
import com.example.numoo.models.AppLimit;
import com.example.numoo.models.UsageData;

import java.util.ArrayList;
import java.util.List;

public class UserDashboardViewModel extends AndroidViewModel {

    private final MutableLiveData<List<UsageData>> usageDataList = new MutableLiveData<>();
    private final MutableLiveData<Long> totalScreenTime = new MutableLiveData<>(0L);
    private final MutableLiveData<List<AppLimit>> appLimits = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private final FirestoreHelper firestoreHelper;
    private final FirebaseAuthHelper authHelper;

    public UserDashboardViewModel(@NonNull Application application) {
        super(application);
        firestoreHelper = new FirestoreHelper(application);
        authHelper = new FirebaseAuthHelper(application);
    }

    public LiveData<List<UsageData>> getUsageDataList() { return usageDataList; }
    public LiveData<Long> getTotalScreenTime() { return totalScreenTime; }
    public LiveData<List<AppLimit>> getAppLimits() { return appLimits; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getError() { return error; }

    public String getUserName() {
        return authHelper.getCachedUserName();
    }

    public void loadUsageData() {
        String uid = authHelper.getCurrentUid();
        if (uid == null) return;

        isLoading.setValue(true);
        String today = FirestoreHelper.getTodayDate();

        firestoreHelper.getUsageDataForDate(uid, today, new FirestoreHelper.FirestoreCallback<List<UsageData>>() {
            @Override
            public void onSuccess(List<UsageData> result) {
                usageDataList.postValue(result != null ? result : new ArrayList<>());
                
                long total = 0;
                if (result != null) {
                    for (UsageData data : result) {
                        total += data.getUsageTimeMillis();
                    }
                }
                totalScreenTime.postValue(total);
                isLoading.postValue(false);
            }

            @Override
            public void onError(String err) {
                error.postValue(err);
                isLoading.postValue(false);
            }
        });
    }

    public void loadLimits() {
        String uid = authHelper.getCurrentUid();
        if (uid == null) return;

        firestoreHelper.listenToLimits(uid, new FirestoreHelper.FirestoreCallback<List<AppLimit>>() {
            @Override
            public void onSuccess(List<AppLimit> result) {
                appLimits.postValue(result);
            }

            @Override
            public void onError(String err) {
                error.postValue(err);
            }
        });
    }

    public void refreshData() {
        loadUsageData();
        loadLimits();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        firestoreHelper.removeLimitsListener();
    }
}
