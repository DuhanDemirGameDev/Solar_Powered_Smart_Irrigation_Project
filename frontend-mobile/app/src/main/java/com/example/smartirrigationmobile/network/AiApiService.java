package com.example.smartirrigationmobile.network;

import com.example.smartirrigationmobile.model.AiPredictionRequest;
import com.example.smartirrigationmobile.model.AiPredictionResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AiApiService {

    @POST("predict")
    Call<AiPredictionResponse> predictIrrigation(@Body AiPredictionRequest request);
}
