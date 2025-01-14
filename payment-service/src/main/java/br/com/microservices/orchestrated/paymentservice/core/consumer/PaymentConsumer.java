package br.com.microservices.orchestrated.paymentservice.core.consumer;

import br.com.microservices.orchestrated.paymentservice.core.service.PaymentService;
import br.com.microservices.orchestrated.paymentservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@AllArgsConstructor
@Component
public class PaymentConsumer {

    private final JsonUtil jsonUtil;
    private final PaymentService paymentService;

    @KafkaListener(
            topics = "${spring.kafka.topic.payment-success}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeSuccessEvent(String payload) {
        log.info("Receiving  success event {} from payment-success topic", payload);
        var event = jsonUtil.toEvent(payload);
        paymentService.realizePayment(event);
    }

    @KafkaListener(
            topics = "${spring.kafka.topic.payment-fail}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeFailEvent(String payload) {
        log.info("Receiving rollback event {} from payment-fail topic", payload);
        var event = jsonUtil.toEvent(payload);
        paymentService.realizeRefound(event);
    }

}
