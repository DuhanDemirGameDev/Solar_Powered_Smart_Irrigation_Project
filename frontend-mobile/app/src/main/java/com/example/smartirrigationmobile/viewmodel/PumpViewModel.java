package com.example.smartirrigationmobile.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class PumpViewModel extends ViewModel {

    private static final long OPTIMISTIC_TTL_MS = 15000L;

    private final MutableLiveData<String> optimisticPumpState = new MutableLiveData<>(null);
    private long optimisticExpiry = 0;

    public void setOptimisticState(String state) {
        optimisticExpiry = System.currentTimeMillis() + OPTIMISTIC_TTL_MS;
        optimisticPumpState.setValue(state);
    }

    public void clearOptimisticState() {
        optimisticExpiry = 0;
        optimisticPumpState.setValue(null);
    }

    public boolean isOptimisticActive() {
        return optimisticPumpState.getValue() != null
                && System.currentTimeMillis() < optimisticExpiry;
    }

    public LiveData<String> getOptimisticPumpState() {
        return optimisticPumpState;
    }
}
