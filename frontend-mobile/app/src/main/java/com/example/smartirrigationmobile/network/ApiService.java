package com.example.smartirrigationmobile.network;

import com.example.smartirrigationmobile.model.IrrigationLog;
import com.example.smartirrigationmobile.model.ManualCommandResponse;
import com.example.smartirrigationmobile.model.PageResponse;
import com.example.smartirrigationmobile.model.PumpCommandRequest;
import com.example.smartirrigationmobile.model.SensorData;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    @GET("api/v1/sensors/history")
    Call<PageResponse<SensorData>> getSensorHistory(
            @Query("page") int page,
            @Query("size") int size
    );

    @POST("api/v1/irrigation/set-command")
    Call<ManualCommandResponse> setPumpCommand(@Body PumpCommandRequest request);

    @GET("api/v1/irrigation/history")
    Call<PageResponse<IrrigationLog>> getIrrigationHistory(
            @Query("page") int page,
            @Query("size") int size
    );
}
