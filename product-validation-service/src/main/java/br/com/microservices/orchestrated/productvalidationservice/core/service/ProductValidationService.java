package br.com.microservices.orchestrated.productvalidationservice.core.service;

import br.com.microservices.orchestrated.productvalidationservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.productvalidationservice.core.dto.Event;
import br.com.microservices.orchestrated.productvalidationservice.core.dto.History;
import br.com.microservices.orchestrated.productvalidationservice.core.dto.OrderProducts;
import br.com.microservices.orchestrated.productvalidationservice.core.enums.ESagaStatus;
import br.com.microservices.orchestrated.productvalidationservice.core.model.Validation;
import br.com.microservices.orchestrated.productvalidationservice.core.producer.KafkaProducer;
import br.com.microservices.orchestrated.productvalidationservice.core.repository.ProductRepository;
import br.com.microservices.orchestrated.productvalidationservice.core.repository.ValidationRepository;
import br.com.microservices.orchestrated.productvalidationservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static org.springframework.util.ObjectUtils.isEmpty;


@Slf4j
@Service
@AllArgsConstructor
public class ProductValidationService {

    private static final String CURRENT_SOURCE = "PRODUCT_VALIDATION_SERVICE";

    private final JsonUtil jsonUtil;
    private final KafkaProducer producer;
    private final ProductRepository productRepository;
    private final ValidationRepository validationRepository;

    public void validateExistingProducts(Event event){
        try {
            checkCurrentValidation(event);
            createValidation(event,true);
            hamdleSuccess(event);
        }catch (Exception e){
            log.error("Error trying to validate existing products", e);
            handleFailCurrentNotExecuted(event,e.getMessage());
        }
        producer.sendEvent(jsonUtil.toJson(event));
    }

    private void checkCurrentValidation(Event event){
        validateProductsInformed(event);
        if(validationRepository.existsByOrderIdAndTransactionId(event.getOrderId(),event.getTrasactionId())){
            throw new ValidationException("There's another transaction for this validation");
        }

        event.getPayload().getProducts().forEach(product -> {
            validadeProductInformed(product);
            validadeExistingProduct(product.getProduct().getCode());
        });
    }

    private void validadeProductInformed(OrderProducts products){
        if (isEmpty(products.getProduct()) || isEmpty(products.getProduct().getCode())) {
            throw new ValidationException("Product  must be informed");
        }
    }

    private void validateProductsInformed(Event event) {
        if (isEmpty(event.getPayload()) || isEmpty(event.getPayload().getProducts())) {
            throw new ValidationException("product list is empty");
        }
        if (isEmpty(event.getPayload().getId()) || isEmpty(event.getPayload().getTransactionId())) {
            throw new ValidationException("OrderID and TransactionID must be informed");
        }
    }

    private void validadeExistingProduct(String code){
            if(productRepository.existsByCode(code)){
                throw new ValidationException("Product does not exist in database");
            }
    }

    private void createValidation(Event event,boolean success){
        var validation = Validation.builder()
                .orderId(event.getPayload().getId())
                .transactionId(event.getTrasactionId())
                .success(success)
                .build();

        validationRepository.save(validation);
    }
    private void hamdleSuccess(Event event){
        event.setStatus(ESagaStatus.SUCCESS);
        event.setSource(CURRENT_SOURCE);
        addHitory(event,"Products are validated successfully!");
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

    private void handleFailCurrentNotExecuted(Event event,String message){
        event.setStatus(ESagaStatus.ROLLBACK_PENDING);
        event.setSource(CURRENT_SOURCE);
        addHitory(event,"Fail to validate products: ".concat(message));
    }
    public void rollbackEvent(Event event){
        changeValidationToFail(event);
        event.setStatus(ESagaStatus.FAIL);
        event.setSource(CURRENT_SOURCE);
        addHitory(event,"Rollback executed on product validation");
        producer.sendEvent(jsonUtil.toJson(event));
    }

    private void changeValidationToFail(Event event){
       validationRepository
               .findByOrderIdAndTransactionId(event.getPayload().getId(),event.getTrasactionId())
                .ifPresentOrElse(validation -> {
                     validation.setSuccess(false);
                     validationRepository.save(validation);
                },() -> createValidation(event,false));
    }
}
