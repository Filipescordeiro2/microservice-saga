package br.com.microservices.orchestrated.inventoryservice.core.service;


import br.com.microservices.orchestrated.inventoryservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.inventoryservice.core.dto.Event;
import br.com.microservices.orchestrated.inventoryservice.core.dto.History;
import br.com.microservices.orchestrated.inventoryservice.core.dto.Order;
import br.com.microservices.orchestrated.inventoryservice.core.dto.OrderProducts;
import br.com.microservices.orchestrated.inventoryservice.core.enums.ESagaStatus;
import br.com.microservices.orchestrated.inventoryservice.core.model.Inventory;
import br.com.microservices.orchestrated.inventoryservice.core.model.OrderInventory;
import br.com.microservices.orchestrated.inventoryservice.core.producer.KafkaProducer;
import br.com.microservices.orchestrated.inventoryservice.core.repository.InventoryRepository;
import br.com.microservices.orchestrated.inventoryservice.core.repository.OrderInventoryRepository;
import br.com.microservices.orchestrated.inventoryservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@AllArgsConstructor
public class InventoryService {

    private static final String CURRENT_SOURCE = "INVENTORY_SERVICE";


    private final JsonUtil jsonUtil;
    private final KafkaProducer producer;
    private final InventoryRepository inventoryRepository;
    private final OrderInventoryRepository orderInventoryRepository;

    public void updateInventory(Event event){
        try{
             checkCurrentValidation(event);
             createOrderInventory(event);
             updateInventory(event.getPayload());
             hamdleSuccess(event);
        }catch (Exception e){
            log.error("Error trying to update inventory", e);
            handleFailCurrentNotExecuted(event,e.getMessage());
        }
        producer.sendEvent(jsonUtil.toJson(event));
    }

    private void createOrderInventory(Event event) {
        event.getPayload().getProducts().forEach(products -> {
            var inventory = findInvetoryByProductCode(products.getProduct().getCode());
            var orderInventory = createOrderInventory(event, products, inventory);
            orderInventoryRepository.save(orderInventory);
        });
    }

    private OrderInventory createOrderInventory(Event event, OrderProducts products, Inventory inventory) {
        return OrderInventory
                .builder()
                .inventory(inventory)
                .oldQuantity(inventory.getAvailable())
                .newQuantity(inventory.getAvailable() - products.getQuantity())
                .orderQuantity(products.getQuantity())
                .orderId(event.getPayload().getId())
                .transactionId(event.getTransactionId()) // Certifique-se de que o transactionId está sendo atribuído corretamente
                .build();
    }

    private Inventory findInvetoryByProductCode(String productCode){
        return inventoryRepository
                .findByProductCode(productCode)
                .orElseThrow(() -> new ValidationException("Inventory not found by informed product code"));
    }

    private void checkCurrentValidation(Event event){
        if(orderInventoryRepository.existsByOrderIdAndTransactionId(event.getOrderId(),event.getTransactionId())){
            throw new ValidationException("There's another transaction for this validation");
        }
    }

    private void updateInventory(Order order){
        order.getProducts().forEach(products ->{
            var inventory = findInvetoryByProductCode(products.getProduct().getCode());
            chekInventory(inventory.getAvailable(),products.getQuantity());
            inventory.setAvailable(inventory.getAvailable() - products.getQuantity());
            inventoryRepository.save(inventory);
        });
    }

    private void chekInventory(int available,int orderQuantity){
        if(orderQuantity > available){
            throw new ValidationException("Product not available in stock");
        }

    }

    private void hamdleSuccess(Event event){
        event.setStatus(ESagaStatus.SUCCESS);
        event.setSource(CURRENT_SOURCE);
        addHitory(event,"Payment realized successfully!");
    }

    private void addHitory(Event event,String message){
        var hisotory = History
                .builder()
                .source(event.getSource())
                .status(event.getStatus())
                .message(message)
                .createAt(LocalDateTime.now())
                .build();

        event.addToHistory(hisotory);
    }

    public void rollbackInventory(Event event){
        event.setStatus(ESagaStatus.FAIL);
        event.setSource(CURRENT_SOURCE);
        try {
            returnInventoryToPreviousValues(event);
            addHitory(event,"Rollback executed for inventory");
        }catch (Exception e){
            log.error("Error trying to make refound", e);
            addHitory(event,"Rollback not executed for inventory".concat(e.getMessage()));
        }
        producer.sendEvent(jsonUtil.toJson(event));
    }

    private void returnInventoryToPreviousValues(Event event) {
        orderInventoryRepository.findByOrderIdAndTransactionId(event.getPayload().getId(),event.getTransactionId())
                .forEach(orderInventory -> {
                    var inventory = orderInventory.getInventory();
                    inventory.setAvailable(orderInventory.getOldQuantity());
                    inventoryRepository.save(inventory);
                    log.info("Restored inventory for order {} from {} to {}",
                            orderInventory.getOrderId(),
                            orderInventory.getNewQuantity(),
                            orderInventory.getOldQuantity());
                });
    }

    private void handleFailCurrentNotExecuted(Event event,String message){
        event.setStatus(ESagaStatus.ROLLBACK_PENDING);
        event.setSource(CURRENT_SOURCE);
        addHitory(event,"Fail to uptade inventory: ".concat(message));
    }

}
