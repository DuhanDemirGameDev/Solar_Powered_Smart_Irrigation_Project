package com.example.backend.repositories;

import com.example.backend.domain.entities.IrrigationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IrrigationLogRepository extends JpaRepository<IrrigationLog, Long> {
}
