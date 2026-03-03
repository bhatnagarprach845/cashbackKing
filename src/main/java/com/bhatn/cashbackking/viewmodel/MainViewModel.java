package com.bhatn.cashbackking.viewmodel;

import android.app.Application;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.amplifyframework.auth.cognito.AWSCognitoAuthSession;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.storage.options.StorageUploadFileOptions;
import com.bhatn.cashbackking.network.CashbackApi;
import com.bhatn.cashbackking.network.RetrofitClient; // Ensure this matches your path

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainViewModel extends AndroidViewModel {
    private final CashbackApi apiClient;

    public MainViewModel(@NonNull Application application) {
        super(application);
        // Initialize your Retrofit client
        this.apiClient = RetrofitClient.getClient("http://10.0.2.2:8080/");
    }

    public void handleBillSelection(Uri uri) {
        File fileToUpload = convertUriToFile(uri);
        if (fileToUpload == null) return;

        String s3Key = "uploads/" + UUID.randomUUID().toString() + ".jpg";

        Amplify.Storage.uploadFile(s3Key, fileToUpload,
                result -> {
                    Log.i("Amplify", "S3 Upload Success: " + result.getKey());
                    // Cleanup local temp file
                    fileToUpload.delete();
                    callBackendToProcess(result.getKey());
                },
                error -> Log.e("Amplify", "S3 Upload Failed", error)
        );
    }

    private void callBackendToProcess(String key) {
        Amplify.Auth.fetchAuthSession(
                session -> {
                    String jwt = ((AWSCognitoAuthSession) session).getUserPoolTokens().getValue().getAccessToken();
                    Map<String, String> request = new HashMap<>();
                    request.put("s3Key", key);

                    apiClient.processBill("Bearer " + jwt, request).enqueue(new Callback<Map<String, String>>() {
                        @Override
                        public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                            if(response.isSuccessful()) {
                                Log.i("Backend", "Bill processing triggered successfully");
                            }
                        }

                        @Override
                        public void onFailure(Call<Map<String, String>> call, Throwable t) {
                            Log.e("Backend", "API Call failed", t);
                        }
                    });
                },
                error -> Log.e("Auth", "Failed to get token", error)
        );
    }

    private File convertUriToFile(Uri uri) {
        File tempFile = new File(getApplication().getCacheDir(), "upload_bill_" + System.currentTimeMillis() + ".jpg");

        try (InputStream inputStream = getApplication().getContentResolver().openInputStream(uri);
             OutputStream outputStream = new FileOutputStream(tempFile)) {

            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            return tempFile;
        } catch (Exception e) {
            Log.e("MainViewModel", "File conversion failed", e);
            return null;
        }
    }
}