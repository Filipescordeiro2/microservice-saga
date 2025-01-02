package br.com.microservices.orchestrated.orchestratorservice.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum ETopics {
    START_SAGA("start-saga"),
    BASE_ORCHESTRATOR("base-orchestrator"),
    FINISH_SUCCESS("finish-success"),
    FINISH_FAIL("finish-fail"),
    PRODUCT_VALIDATION_SUCCESS("finish-product-validation-success"),
    PRODUCT_VALIDATION_FAIL("finish-product-validation-fail"),
    PAYMENT_SUCCESS("finish-payment-success"),
    PAYMENT_FAIL("finish-payment-fail"),
    INVENTORY_SUCCESS("finish-inventory-success"),
    INVENTORY_FAIL("finish-inventory-fail"),
    NOTIFY_ENDING("notify-ending");

    private String topic;
}
