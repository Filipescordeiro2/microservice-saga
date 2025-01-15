package br.com.microservices.orchestrated.inventoryservice.core.consumer;

import br.com.microservices.orchestrated.inventoryservice.core.service.InventoryService;
import br.com.microservices.orchestrated.inventoryservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@AllArgsConstructor
@Component
public class InventoryConsumer {

    private final JsonUtil jsonUtil;
    private final InventoryService inventoryService;



    @KafkaListener(
            topics = "${spring.kafka.topic.inventory-success}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeSuccessEvent(String payload) {
        log.info("Receiving  success event {} from inventory-success topic", payload);
        var event = jsonUtil.toEvent(payload);
        inventoryService.updateInventory(event);
    }

    @KafkaListener(
            topics = "${spring.kafka.topic.inventory-fail}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeFailEvent(String payload) {
        log.info("Receiving rollback event {} from inventory-fail topic", payload);
        var event = jsonUtil.toEvent(payload);
        inventoryService.rollbackInventory(event);
    }


}
