package com.example.smartirrigationmobile.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class AiRetrofitClient {

    private static final String AI_BASE_URL = "http://10.0.2.2:5000/";
    private static volatile Retrofit retrofit;

    private AiRetrofitClient() {
    }

    public static Retrofit getInstance() {
        if (retrofit == null) {
            synchronized (AiRetrofitClient.class) {
                if (retrofit == null) {
                    retrofit = new Retrofit.Builder()
                            .baseUrl(AI_BASE_URL)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                }
            }
        }

        return retrofit;
    }

    public static AiApiService getAiApiService() {
        return getInstance().create(AiApiService.class);
    }
}
