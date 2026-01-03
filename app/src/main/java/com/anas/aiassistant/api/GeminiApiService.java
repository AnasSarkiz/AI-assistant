package com.anas.aiassistant.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface GeminiApiService {
    @POST("chat/completions")
    Call<GeminiResponse> generateChat(
        @Header("Authorization") String authorization,
        @Body GeminiRequest request
    );
}