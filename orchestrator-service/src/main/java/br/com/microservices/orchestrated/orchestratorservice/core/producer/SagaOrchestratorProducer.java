package br.com.microservices.orchestrated.orchestratorservice.core.producer;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@AllArgsConstructor
@Component
public class SagaOrchestratorProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendEvent(String paylod,String topic) {

        try {
            log.info("Sending event to topic: {} with data {}", topic,paylod);
            kafkaTemplate.send(topic, paylod);
        } catch (Exception e) {
            log.error("Error trying to send event to topic: {} with data {}", topic,paylod, e);
        }
    }
}
