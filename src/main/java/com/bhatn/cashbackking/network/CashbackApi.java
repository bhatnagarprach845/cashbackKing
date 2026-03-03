package com.bhatn.cashbackking.network;
import com.bhatn.cashbackking.dto.PayoutStatusResponse;
import com.bhatn.cashbackking.dto.UserResponse;
// IMPORTANT: Use retrofit2 imports, not okhttp3
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

import java.util.Map;

public interface CashbackApi {
    @POST("api/v1/users/onboard")
    Call<UserResponse> onboardUser(@Header("Authorization") String token, @Body Map<String, String> body);

    @POST("api/v1/bills/process")
    Call<Map<String, String>> processBill(@Header("Authorization") String token, @Body Map<String, String> body);

    @GET("api/v1/users/status")
    Call<PayoutStatusResponse> getWalletStatus(@Header("Authorization") String token);
}