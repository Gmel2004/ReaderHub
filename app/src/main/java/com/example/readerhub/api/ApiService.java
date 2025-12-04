package com.example.readerhub.api;

import com.example.readerhub.api.models.AuthResponse;
import com.example.readerhub.api.models.GenericResponse;
import com.example.readerhub.api.models.LoginRequest;
import com.example.readerhub.api.models.RegisterRequest;
import com.example.readerhub.api.models.SyncDataRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ApiService {

    @POST("auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    @POST("auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @GET("user/profile")
    Call<AuthResponse> getUserProfile(@Header("Authorization") String token);

    @POST("sync/upload")
    Call<GenericResponse> syncData(@Header("Authorization") String token,
                                   @Body SyncDataRequest request);

    @GET("sync/download/{userId}")
    Call<SyncDataRequest> downloadSyncData(@Header("Authorization") String token,
                                           @Path("userId") int userId);

    @PUT("user/settings")
    Call<GenericResponse> updateSettings(@Header("Authorization") String token,
                                         @Body String settings);
}