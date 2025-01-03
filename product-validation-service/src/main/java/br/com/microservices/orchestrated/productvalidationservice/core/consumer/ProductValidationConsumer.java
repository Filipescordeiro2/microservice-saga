package br.com.microservices.orchestrated.productvalidationservice.core.consumer;

import br.com.microservices.orchestrated.productvalidationservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@AllArgsConstructor
@Component
public class ProductValidationConsumer {

    private final JsonUtil jsonUtil;


    @KafkaListener(
            topics = "${spring.kafka.topic.product-validation-success}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeSuccessEvent(String payload) {
        log.info("Receiving  success event {} from product-validation-success topic", payload);
        var event = jsonUtil.toEvent(payload);
        log.info(event.toString());
    }

    @KafkaListener(
            topics = "${spring.kafka.topic.product-validation-fail}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeFailEvent(String payload) {
        log.info("Receiving rollback event {} from product-validation-fail topic", payload);
        var event = jsonUtil.toEvent(payload);
        log.info(event.toString());
    }


}
