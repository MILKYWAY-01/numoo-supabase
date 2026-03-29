package com.example.numoo.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.numoo.firebase.FirebaseAuthHelper;

public class AuthViewModel extends AndroidViewModel {

    private final FirebaseAuthHelper authHelper;
    private final MutableLiveData<String> authResult = new MutableLiveData<>();
    private final MutableLiveData<String> authError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public AuthViewModel(@NonNull Application application) {
        super(application);
        authHelper = new FirebaseAuthHelper(application);
    }

    public LiveData<String> getAuthResult() { return authResult; }
    public LiveData<String> getAuthError() { return authError; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    public boolean isLoggedIn() {
        return authHelper.isLoggedIn();
    }

    public String getCachedRole() {
        return authHelper.getCachedRole();
    }

    public void login(String email, String password) {
        isLoading.setValue(true);
        authHelper.login(email, password, new FirebaseAuthHelper.AuthCallback() {
            @Override
            public void onSuccess(String role) {
                isLoading.postValue(false);
                authResult.postValue(role);
            }

            @Override
            public void onError(String error) {
                isLoading.postValue(false);
                authError.postValue(error);
            }
        });
    }

    public void registerAdmin(String name, String username, String email, String password) {
        isLoading.setValue(true);
        authHelper.registerAdmin(name, username, email, password, new FirebaseAuthHelper.AuthCallback() {
            @Override
            public void onSuccess(String adminCode) {
                isLoading.postValue(false);
                authResult.postValue("ADMIN_CODE:" + adminCode);
            }

            @Override
            public void onError(String error) {
                isLoading.postValue(false);
                authError.postValue(error);
            }
        });
    }

    public void registerUser(String name, String username, String email,
                             String password, String adminCode) {
        isLoading.setValue(true);
        authHelper.registerUser(name, username, email, password, adminCode,
            new FirebaseAuthHelper.AuthCallback() {
                @Override
                public void onSuccess(String message) {
                    isLoading.postValue(false);
                    authResult.postValue("USER_REGISTERED");
                }

                @Override
                public void onError(String error) {
                    isLoading.postValue(false);
                    authError.postValue(error);
                }
            });
    }

    public void logout() {
        authHelper.logout();
    }
}
