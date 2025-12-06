package com.example.physiciansservice.event;

import com.example.physiciansservice.model.Physician;
import com.example.physiciansservice.repository.PhysicianRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;


@Service
public class PhysicianConsumer {

    private static final Logger logger = LoggerFactory.getLogger(PhysicianConsumer.class);
    private final PhysicianRepository repository;

    public PhysicianConsumer(PhysicianRepository repository) {
        this.repository = repository;
    }

    // Escuta a fila criada no RabbitMQConfig (syncQueue)
    // A expressao SpEL "#{syncQueue.name}" encontra o nome aleatorio gerado
    @RabbitListener(queues = "#{syncQueue.name}")
    public void receivePhysicianEvent(PhysicianEvent event) {
        logger.info("--> RabbitMQ: Recebi evento {} para {}", event.getEventType(), event.getPhysicianNumber());

        switch (event.getEventType()) {
            case "CREATED":
                handleCreate(event);
                break;
            case "UPDATED":
                handleUpdate(event);
                break;
            case "DELETED":
                handleDelete(event);
                break;
            default:
                logger.warn("Evento desconhecido: {}", event.getEventType());
        }
    }

    private void handleCreate(PhysicianEvent event) {
        if (repository.findByPhysicianNumber(event.getPhysicianNumber()).isEmpty()) {
            Physician newP = new Physician();
            newP.setPhysicianNumber(event.getPhysicianNumber());
            newP.setName(event.getName());
            newP.setSpecialty(event.getSpecialty());
            newP.setContactInfo(event.getContactInfo());
            repository.save(newP);
            logger.info("SINC: Médico {} criado.", event.getPhysicianNumber());
        } else {
            logger.info("SINC: Médico {} já existe. Ignorar Create.", event.getPhysicianNumber());
        }
    }

    private void handleUpdate(PhysicianEvent event) {
        // Tenta encontrar o médico para atualizar
        repository.findByPhysicianNumber(event.getPhysicianNumber()).ifPresentOrElse(
                p -> {
                    p.setName(event.getName());
                    p.setSpecialty(event.getSpecialty());
                    p.setContactInfo(event.getContactInfo());
                    repository.save(p);
                    logger.info("SINC: Médico {} atualizado.", event.getPhysicianNumber());
                },
                () -> {
                    // Se recebermos um UPDATE para algo que nao temos, podemos criar (Upsert) ou ignorar
                    Physician newP = new Physician();
                    newP.setPhysicianNumber(event.getPhysicianNumber());
                    newP.setName(event.getName());
                    newP.setSpecialty(event.getSpecialty());
                    newP.setContactInfo(event.getContactInfo());
                    repository.save(newP);
                    logger.info("SINC: Médico {} não existia no Update. Criado agora.", event.getPhysicianNumber());
                }
        );
    }

    private void handleDelete(PhysicianEvent event) {
        repository.findByPhysicianNumber(event.getPhysicianNumber()).ifPresentOrElse(
                p -> {
                    repository.delete(p);
                    logger.info("SINC: Médico {} apagado.", event.getPhysicianNumber());
                },
                () -> logger.info("SINC: Médico {} já não existia. Ignorar Delete.", event.getPhysicianNumber())
        );
    }
}
