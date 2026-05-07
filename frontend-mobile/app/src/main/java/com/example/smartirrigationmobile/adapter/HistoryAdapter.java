package com.example.smartirrigationmobile.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartirrigationmobile.R;
import com.example.smartirrigationmobile.model.IrrigationLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private final List<IrrigationLog> logs = new ArrayList<>();

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        holder.bind(logs.get(position));
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    public void submitList(List<IrrigationLog> newLogs) {
        logs.clear();
        if (newLogs != null) {
            logs.addAll(newLogs);
        }
        notifyDataSetChanged();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {

        private final TextView statusText;
        private final TextView durationText;
        private final TextView timestampText;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            statusText = itemView.findViewById(R.id.text_history_status);
            durationText = itemView.findViewById(R.id.text_history_duration);
            timestampText = itemView.findViewById(R.id.text_history_timestamp);
        }

        void bind(IrrigationLog log) {
            String pumpStatus = log.getPumpStatus() == null ? "UNKNOWN" : log.getPumpStatus();
            Integer duration = log.getDurationInMinutes();

            statusText.setText(String.format(Locale.getDefault(), "Pump %s", pumpStatus));
            durationText.setText(String.format(
                    Locale.getDefault(),
                    "%d min",
                    duration == null ? 0 : duration
            ));
            timestampText.setText(log.getTimestamp() == null ? "No timestamp" : log.getTimestamp());
        }
    }
}
