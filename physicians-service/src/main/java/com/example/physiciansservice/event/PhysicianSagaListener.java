package com.example.physiciansservice.event;

import com.example.physiciansservice.event.dto.AppointmentEventDTO;
import com.example.physiciansservice.event.dto.PhysicianReservedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PhysicianSagaListener {

    private final RabbitTemplate rabbitTemplate;

    public PhysicianSagaListener(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    // Ouve a criação de consultas na fila "saga-physician-appointment-listener"
    @RabbitListener(queues = "saga-physician-appointment-listener")
    public void onAppointmentCreated(AppointmentEventDTO event) {
        System.out.println("PHYSICIAN SAGA: Recebi pedido para consulta " + event.getAppointmentNumber());
        System.out.println("Verificando disponibilidade para médico: " + event.getPhysicianNumber());

        // Lógica de Negócio: Verificar Disponibilidade
        boolean isAvailable = checkAvailability(event.getPhysicianNumber());

        // Criar a resposta
        PhysicianReservedEvent response = new PhysicianReservedEvent();
        response.setAppointmentNumber(event.getAppointmentNumber());
        response.setPhysicianNumber(event.getPhysicianNumber());
        response.setAvailable(isAvailable);

        // Enviar para a fila de resposta do AppointmentService
        rabbitTemplate.convertAndSend("physician-response-queue", response);

        System.out.println("PHYSICIAN SAGA: Enviei resposta (Disponível: " + isAvailable + ")");
    }


    private boolean checkAvailability(String physicianId) {
        // SIMULAÇÃO: Se o ID for "P-FAIL", rejeita (para testares o fluxo de falha)
        if ("P-FAIL".equals(physicianId)) {
            return false;
        }
        // Caso contrário, aceita sempre
        return true;
    }
}