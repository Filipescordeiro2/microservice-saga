package br.com.microservices.orchestrated.orchestratorservice.core.consumer;

import br.com.microservices.orchestrated.orchestratorservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@AllArgsConstructor
@Component
public class SagaOrchestratorConsumer {

    private final JsonUtil jsonUtil;


    @KafkaListener(
            topics = "${spring.kafka.topic.start-saga}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeStartSagaEvent(String payload) {
        log.info("Receiving  event {} from start-saga topic", payload);
        var event = jsonUtil.toEvent(payload);
        log.info(event.toString());
    }

    @KafkaListener(
            topics = "${spring.kafka.topic.orchestrator}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeOrchestratorEvent(String payload) {
        log.info("Receiving  event {} from orchestrator topic", payload);
        var event = jsonUtil.toEvent(payload);
        log.info(event.toString());
    }

    @KafkaListener(
            topics = "${spring.kafka.topic.finish-success}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeFinishSuccessEvent(String payload) {
        log.info("Receiving  event {} from finish-success topic", payload);
        var event = jsonUtil.toEvent(payload);
        log.info(event.toString());
    }

    @KafkaListener(
            topics = "${spring.kafka.topic.finish-fail}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeFinishFailEvent(String payload) {
        log.info("Receiving  event {} from finish-fail topic", payload);
        var event = jsonUtil.toEvent(payload);
        log.info(event.toString());
    }
}
