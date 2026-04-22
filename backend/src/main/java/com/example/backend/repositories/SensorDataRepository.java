package com.example.backend.repositories;

import com.example.backend.domain.entities.SensorData;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SensorDataRepository extends JpaRepository<SensorData, Long> {

    List<SensorData> findTop10ByOrderByTimestampDesc();
}
