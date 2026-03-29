package com.example.numoo.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.numoo.firebase.FirebaseAuthHelper;
import com.example.numoo.firebase.FirestoreHelper;
import com.example.numoo.models.User;

import java.util.ArrayList;
import java.util.List;

public class AdminDashboardViewModel extends AndroidViewModel {

    private final MutableLiveData<List<User>> linkedUsers = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<String> adminCode = new MutableLiveData<>();

    private final FirestoreHelper firestoreHelper;
    private final FirebaseAuthHelper authHelper;

    public AdminDashboardViewModel(@NonNull Application application) {
        super(application);
        firestoreHelper = new FirestoreHelper(application);
        authHelper = new FirebaseAuthHelper(application);
    }

    public LiveData<List<User>> getLinkedUsers() { return linkedUsers; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getError() { return error; }
    public LiveData<String> getAdminCode() { return adminCode; }

    public String getAdminName() {
        return authHelper.getCachedUserName();
    }

    public void loadLinkedUsers() {
        isLoading.setValue(true);
        String uid = authHelper.getCurrentUid();
        if (uid == null) {
            error.setValue("Not logged in");
            isLoading.setValue(false);
            return;
        }

        // Load admin code
        firestoreHelper.getUserInfo(uid, new FirestoreHelper.FirestoreCallback<User>() {
            @Override
            public void onSuccess(User result) {
                if (result != null && result.getAdminCode() != null) {
                    adminCode.postValue(result.getAdminCode());
                }
            }

            @Override
            public void onError(String err) {
                // Non-critical
            }
        });

        firestoreHelper.getLinkedUsers(uid, new FirestoreHelper.FirestoreCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> result) {
                linkedUsers.postValue(result != null ? result : new ArrayList<>());
                isLoading.postValue(false);
            }

            @Override
            public void onError(String err) {
                error.postValue(err);
                isLoading.postValue(false);
            }
        });
    }

    public void logout() {
        authHelper.logout();
    }
}
