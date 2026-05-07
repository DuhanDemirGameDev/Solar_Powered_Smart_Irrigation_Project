package com.example.smartirrigationmobile.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smartirrigationmobile.R;
import com.example.smartirrigationmobile.model.AiPredictionRequest;
import com.example.smartirrigationmobile.model.AiPredictionResponse;
import com.example.smartirrigationmobile.model.PageResponse;
import com.example.smartirrigationmobile.model.SensorData;
import com.example.smartirrigationmobile.network.AiRetrofitClient;
import com.example.smartirrigationmobile.network.RetrofitClient;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeatherFragment extends Fragment {

    private TextView aiStatusText;
    private TextView decisionText;
    private TextView decisionCopyText;
    private TextView rainProbabilityText;
    private TextView temperatureText;
    private TextView humidityText;
    private TextView payloadText;

    private Call<PageResponse<SensorData>> latestSensorCall;
    private Call<AiPredictionResponse> predictionCall;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_weather, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        aiStatusText = view.findViewById(R.id.text_ai_status);
        decisionText = view.findViewById(R.id.text_ai_decision);
        decisionCopyText = view.findViewById(R.id.text_ai_decision_copy);
        rainProbabilityText = view.findViewById(R.id.text_rain_probability);
        temperatureText = view.findViewById(R.id.text_weather_temperature);
        humidityText = view.findViewById(R.id.text_weather_humidity);
        payloadText = view.findViewById(R.id.text_ai_payload);

        fetchLatestSensorData();
    }

    private void fetchLatestSensorData() {
        setLoadingState();
        latestSensorCall = RetrofitClient.getApiService().getLatestSensorData();
        latestSensorCall.enqueue(new Callback<PageResponse<SensorData>>() {
            @Override
            public void onResponse(@NonNull Call<PageResponse<SensorData>> call,
                                   @NonNull Response<PageResponse<SensorData>> response) {
                if (!isAdded()) {
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    List<SensorData> sensorDataList = response.body().getContent();
                    if (!sensorDataList.isEmpty()) {
                        requestPrediction(sensorDataList.get(0));
                        return;
                    }
                }

                showUnavailableState("No sensor data available");
            }

            @Override
            public void onFailure(@NonNull Call<PageResponse<SensorData>> call, @NonNull Throwable t) {
                if (!call.isCanceled() && isAdded()) {
                    showUnavailableState("Backend connection failed");
                }
            }
        });
    }

    private void requestPrediction(SensorData sensorData) {
        double moisture = sensorData.getMoisturePercent() == null ? 50.0 : sensorData.getMoisturePercent();
        boolean isRaining = Boolean.TRUE.equals(sensorData.getRaining());

        payloadText.setText(String.format(
                Locale.getDefault(),
                "moisture: %.1f, is_raining: %s",
                moisture,
                isRaining
        ));

        predictionCall = AiRetrofitClient.getAiApiService()
                .predictIrrigation(new AiPredictionRequest(moisture, isRaining));
        predictionCall.enqueue(new Callback<AiPredictionResponse>() {
            @Override
            public void onResponse(@NonNull Call<AiPredictionResponse> call,
                                   @NonNull Response<AiPredictionResponse> response) {
                if (!isAdded()) {
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    bindPrediction(response.body());
                } else {
                    showUnavailableState("AI service rejected the request");
                }
            }

            @Override
            public void onFailure(@NonNull Call<AiPredictionResponse> call, @NonNull Throwable t) {
                if (!call.isCanceled() && isAdded()) {
                    showUnavailableState("Start Flask API on 10.0.2.2:5000");
                }
            }
        });
    }

    private void bindPrediction(AiPredictionResponse prediction) {
        String decision = prediction.getDecision() == null ? "--" : prediction.getDecision().toUpperCase(Locale.US);
        aiStatusText.setText("AI live");
        decisionText.setText(decision);
        decisionCopyText.setText(getDecisionCopy(decision));
        rainProbabilityText.setText(formatPercent(prediction.getRainProbability()));
        temperatureText.setText(formatTemperature(prediction.getTemperature()));
        humidityText.setText(formatPercent(prediction.getHumidity()));
    }

    private void setLoadingState() {
        aiStatusText.setText("AI syncing");
        decisionText.setText("--");
        decisionCopyText.setText("Waiting for weather and prediction data.");
        rainProbabilityText.setText("--%");
        temperatureText.setText("-- C");
        humidityText.setText("--%");
        payloadText.setText("moisture: 50.0, is_raining: false");
    }

    private void showUnavailableState(String message) {
        aiStatusText.setText("AI offline");
        decisionText.setText("--");
        decisionCopyText.setText(message);
        rainProbabilityText.setText("--%");
        temperatureText.setText("-- C");
        humidityText.setText("--%");
    }

    private String getDecisionCopy(String decision) {
        if (decision.contains("IRRIGATE") || decision.contains("WATER") || "ON".equals(decision)) {
            return "Soil and weather signals support irrigation now.";
        }

        if (decision.contains("POSTPONE") || decision.contains("WAIT") || decision.contains("OFF")) {
            return "Weather or moisture conditions suggest delaying irrigation.";
        }

        return "Latest model output is ready for review.";
    }

    private String formatPercent(Double value) {
        return value == null ? "--%" : String.format(Locale.getDefault(), "%.1f%%", value);
    }

    private String formatTemperature(Double value) {
        return value == null ? "-- C" : String.format(Locale.getDefault(), "%.1f C", value);
    }

    @Override
    public void onDestroyView() {
        if (latestSensorCall != null) {
            latestSensorCall.cancel();
        }
        if (predictionCall != null) {
            predictionCall.cancel();
        }
        super.onDestroyView();
    }
}
