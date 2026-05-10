package com.example.smartirrigationmobile.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.smartirrigationmobile.R;
import com.example.smartirrigationmobile.model.PageResponse;
import com.example.smartirrigationmobile.model.SensorData;
import com.example.smartirrigationmobile.network.RetrofitClient;
import com.example.smartirrigationmobile.viewmodel.PumpViewModel;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardFragment extends Fragment {

    private static final long POLL_INTERVAL_MS = 15000L;

    private TextView moistureValueText;
    private TextView rainStatusText;
    private TextView pumpStateText;
    private TextView lastUpdatedText;
    private Call<PageResponse<SensorData>> latestSensorCall;
    private PumpViewModel pumpViewModel;

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAdded()) {
                fetchLatestSensorData();
                pollHandler.postDelayed(this, POLL_INTERVAL_MS);
            }
        }
    };

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

        pumpViewModel = new ViewModelProvider(requireActivity()).get(PumpViewModel.class);
        pumpViewModel.getOptimisticPumpState().observe(getViewLifecycleOwner(), optimisticState -> {
            if (pumpViewModel.isOptimisticActive()) {
                pumpStateText.setText(optimisticState);
            }
        });

        fetchLatestSensorData();
        pollHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
    }

    private void fetchLatestSensorData() {
        if (latestSensorCall != null) {
            latestSensorCall.cancel();
        }
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

        String normalizedState = normalizePumpState(sensorData.getPumpState());
        if (pumpViewModel.isOptimisticActive()) {
            if ("ON".equals(normalizedState)) {
                pumpViewModel.clearOptimisticState();
                pumpStateText.setText(normalizedState);
            }
        } else {
            pumpStateText.setText(normalizedState);
        }

        lastUpdatedText.setText(sensorData.getTimestamp() == null ? "No timestamp" : sensorData.getTimestamp());
    }

    private String normalizePumpState(String raw) {
        if (raw == null) return "Unknown";
        switch (raw.toUpperCase()) {
            case "RUNNING": return "ON";
            case "IDLE":    return "OFF";
            default:        return raw;
        }
    }

    private void showUnavailableState() {
        moistureValueText.setText("--%");
        rainStatusText.setText("Unavailable");
        pumpStateText.setText("Unavailable");
        lastUpdatedText.setText("Check backend connection");
    }

    @Override
    public void onDestroyView() {
        pollHandler.removeCallbacks(pollRunnable);
        if (latestSensorCall != null) {
            latestSensorCall.cancel();
        }
        super.onDestroyView();
    }
}
