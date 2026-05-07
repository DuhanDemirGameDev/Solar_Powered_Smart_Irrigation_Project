package com.example.smartirrigationmobile.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartirrigationmobile.R;
import com.example.smartirrigationmobile.adapter.HistoryAdapter;
import com.example.smartirrigationmobile.model.IrrigationLog;
import com.example.smartirrigationmobile.model.PageResponse;
import com.example.smartirrigationmobile.network.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryFragment extends Fragment {

    private HistoryAdapter historyAdapter;
    private TextView emptyText;
    private Call<PageResponse<IrrigationLog>> historyCall;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView historyRecyclerView = view.findViewById(R.id.recycler_history);
        emptyText = view.findViewById(R.id.text_history_empty);

        historyAdapter = new HistoryAdapter();
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        historyRecyclerView.setAdapter(historyAdapter);

        fetchHistory();
    }

    private void fetchHistory() {
        historyCall = RetrofitClient.getApiService().getIrrigationHistory(0, 30);
        historyCall.enqueue(new Callback<PageResponse<IrrigationLog>>() {
            @Override
            public void onResponse(@NonNull Call<PageResponse<IrrigationLog>> call,
                                   @NonNull Response<PageResponse<IrrigationLog>> response) {
                if (!isAdded() || getView() == null) {
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    List<IrrigationLog> logs = response.body().getContent();
                    historyAdapter.submitList(logs);
                    emptyText.setVisibility(logs.isEmpty() ? View.VISIBLE : View.GONE);
                    return;
                }

                showEmptyMessage("Could not load history");
            }

            @Override
            public void onFailure(@NonNull Call<PageResponse<IrrigationLog>> call, @NonNull Throwable t) {
                if (!call.isCanceled() && isAdded()) {
                    showEmptyMessage("Check backend connection");
                }
            }
        });
    }

    private void showEmptyMessage(String message) {
        historyAdapter.submitList(null);
        emptyText.setText(message);
        emptyText.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        if (historyCall != null) {
            historyCall.cancel();
        }
        super.onDestroyView();
    }
}
