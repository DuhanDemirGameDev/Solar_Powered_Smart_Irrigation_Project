package com.example.smartirrigationmobile.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.smartirrigationmobile.R;
import com.example.smartirrigationmobile.model.ManualCommandResponse;
import com.example.smartirrigationmobile.model.PumpCommandRequest;
import com.example.smartirrigationmobile.network.RetrofitClient;
import com.example.smartirrigationmobile.viewmodel.PumpViewModel;
import com.google.android.material.button.MaterialButton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ControlFragment extends Fragment {

    private static final int START_DURATION_SECONDS = 15;

    private MaterialButton startPumpButton;
    private MaterialButton stopPumpButton;
    private Call<ManualCommandResponse> commandCall;
    private PumpViewModel pumpViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_control, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        pumpViewModel = new ViewModelProvider(requireActivity()).get(PumpViewModel.class);

        startPumpButton = view.findViewById(R.id.button_start_pump);
        stopPumpButton = view.findViewById(R.id.button_stop_pump);

        startPumpButton.setOnClickListener(v -> sendPumpCommand(
                new PumpCommandRequest("start", START_DURATION_SECONDS, "Manual mobile start"),
                "ON"
        ));
        stopPumpButton.setOnClickListener(v -> sendPumpCommand(
                new PumpCommandRequest("stop", 0, "Manual mobile stop"),
                null
        ));
    }

    private void sendPumpCommand(PumpCommandRequest request, @Nullable String optimisticState) {
        setButtonsEnabled(false);
        commandCall = RetrofitClient.getApiService().setPumpCommand(request);
        commandCall.enqueue(new Callback<ManualCommandResponse>() {
            @Override
            public void onResponse(@NonNull Call<ManualCommandResponse> call,
                                   @NonNull Response<ManualCommandResponse> response) {
                if (!isAdded()) {
                    return;
                }

                setButtonsEnabled(true);
                if (response.isSuccessful()) {
                    if (optimisticState != null) {
                        pumpViewModel.setOptimisticState(optimisticState);
                    } else {
                        pumpViewModel.clearOptimisticState();
                    }
                    Toast.makeText(requireContext(), "Pump command sent", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Command rejected by server", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ManualCommandResponse> call, @NonNull Throwable t) {
                if (!call.isCanceled() && isAdded()) {
                    setButtonsEnabled(true);
                    Toast.makeText(requireContext(), "Connection failed", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setButtonsEnabled(boolean enabled) {
        startPumpButton.setEnabled(enabled);
        stopPumpButton.setEnabled(enabled);
    }

    @Override
    public void onDestroyView() {
        if (commandCall != null) {
            commandCall.cancel();
        }
        super.onDestroyView();
    }
}
