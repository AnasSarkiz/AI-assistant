package com.anas.aiassistant.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ApiService {
    @POST("chat/completions")
    Call<Response> generateChat(
        @Header("Authorization") String authorization,
        @Body Request request
    );
}