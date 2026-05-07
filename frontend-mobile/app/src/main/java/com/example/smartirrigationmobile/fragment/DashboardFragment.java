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
import com.example.smartirrigationmobile.model.PageResponse;
import com.example.smartirrigationmobile.model.SensorData;
import com.example.smartirrigationmobile.network.RetrofitClient;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardFragment extends Fragment {

    private TextView moistureValueText;
    private TextView rainStatusText;
    private TextView pumpStateText;
    private TextView lastUpdatedText;
    private Call<PageResponse<SensorData>> latestSensorCall;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        moistureValueText = view.findViewById(R.id.text_soil_moisture_value);
        rainStatusText = view.findViewById(R.id.text_rain_status_value);
        pumpStateText = view.findViewById(R.id.text_pump_state_value);
        lastUpdatedText = view.findViewById(R.id.text_last_updated_value);

        fetchLatestSensorData();
    }

    private void fetchLatestSensorData() {
        latestSensorCall = RetrofitClient.getApiService().getLatestSensorData();
        latestSensorCall.enqueue(new Callback<PageResponse<SensorData>>() {
            @Override
            public void onResponse(@NonNull Call<PageResponse<SensorData>> call,
                                   @NonNull Response<PageResponse<SensorData>> response) {
                if (!isAdded() || getView() == null) {
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    List<SensorData> sensorDataList = response.body().getContent();
                    if (!sensorDataList.isEmpty()) {
                        bindSensorData(sensorDataList.get(0));
                        return;
                    }
                }

                showUnavailableState();
            }

            @Override
            public void onFailure(@NonNull Call<PageResponse<SensorData>> call, @NonNull Throwable t) {
                if (!call.isCanceled() && isAdded()) {
                    showUnavailableState();
                }
            }
        });
    }

    private void bindSensorData(SensorData sensorData) {
        Double moisturePercent = sensorData.getMoisturePercent();
        Boolean isRaining = sensorData.getRaining();

        moistureValueText.setText(moisturePercent == null
                ? "--%"
                : String.format(Locale.getDefault(), "%.1f%%", moisturePercent));
        rainStatusText.setText(Boolean.TRUE.equals(isRaining) ? "Raining" : "No Rain");
        pumpStateText.setText(sensorData.getPumpState() == null ? "Unknown" : sensorData.getPumpState());
        lastUpdatedText.setText(sensorData.getTimestamp() == null ? "No timestamp" : sensorData.getTimestamp());
    }

    private void showUnavailableState() {
        moistureValueText.setText("--%");
        rainStatusText.setText("Unavailable");
        pumpStateText.setText("Unavailable");
        lastUpdatedText.setText("Check backend connection");
    }

    @Override
    public void onDestroyView() {
        if (latestSensorCall != null) {
            latestSensorCall.cancel();
        }
        super.onDestroyView();
    }
}
