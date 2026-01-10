package com.example.appointmentservice.repository;

import com.example.appointmentservice.model.AppointmentEventStore;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AppointmentEventRepository extends JpaRepository<AppointmentEventStore, Long> {

    // QUERY METHOD: Encontra todos os eventos de um ID específico
    // Ordena do mais antigo para o mais recente para o "Replay" funcionar corretamente
    List<AppointmentEventStore> findByAppointmentNumberOrderByOccurredAtAsc(String appointmentNumber);
}