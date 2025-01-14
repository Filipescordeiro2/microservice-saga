package br.com.microservices.orchestrated.paymentservice.core.service;

import br.com.microservices.orchestrated.paymentservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.paymentservice.core.dto.Event;
import br.com.microservices.orchestrated.paymentservice.core.dto.History;
import br.com.microservices.orchestrated.paymentservice.core.dto.OrderProducts;
import br.com.microservices.orchestrated.paymentservice.core.enums.EPaymentStatus;
import br.com.microservices.orchestrated.paymentservice.core.enums.ESagaStatus;
import br.com.microservices.orchestrated.paymentservice.core.model.Payment;
import br.com.microservices.orchestrated.paymentservice.core.producer.KafkaProducer;
import br.com.microservices.orchestrated.paymentservice.core.repository.PaymentRepository;
import br.com.microservices.orchestrated.paymentservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@AllArgsConstructor
public class PaymentService {

    private static final String CURRENT_SOURCE = "PAYMENTE_SERVICE";
    private static final Double REDUCE_SUM_VALUE = 0.0;
    private static final Double MIN_AMOUNT_VALUE = 0.1;


    private final JsonUtil jsonUtil;
    private final KafkaProducer producer;
    private final PaymentRepository repository;

    public void realizePayment(Event event){
        try{
                checkCurrentValidation(event);
                createPedingPayment(event);
                var payment = findByOrderIdAndTransactionId(event);
                validadeAmount(payment.getTotalAmount());
                changePaymentToSucess(payment);
                hamdleSuccess(event);
        }catch (Exception e){
            log.error("Error trying to make payment", e);
            handleFailCurrentNotExecuted(event,e.getMessage());
        }
        producer.sendEvent(jsonUtil.toJson(event));
    }

    private void createPedingPayment(Event event) {
        var totalItems = calculateTotalItems(event);
        var totalAmount = calculateTotalAmount(event);

        var payment = Payment
                .builder()
                .orderId(event.getPayload().getId())
                .transactionId(event.getTransactionId())
                .totalItems(totalItems)
                .totalAmount(totalAmount)
                        .build();

        savePayment(payment);
        setEventAmountItems(event,payment);
    }

    private double calculateTotalAmount(Event event){

        return event
                .getPayload()
                .getProducts()
                .stream()
                .map(product -> product.getQuantity() * product.getProduct().getUnitValue())
                .reduce(REDUCE_SUM_VALUE, Double::sum);
    }

    private int calculateTotalItems(Event event){
        return event
                .getPayload()
                .getProducts()
                .stream()
                .map(OrderProducts::getQuantity)
                .reduce(REDUCE_SUM_VALUE.intValue(), Integer::sum);
    }

    private void savePayment(Payment payment){
        repository.save(payment);
    }

    private Payment findByOrderIdAndTransactionId(Event event){
        return repository.findByOrderIdAndTransactionId(event.getPayload().getId(),event.getTransactionId())
                .orElseThrow(() -> new ValidationException("Payment not found by OrderId and TransactionId"));
    }
    private void validadeAmount(double totalAmount){
        if (totalAmount <0.1){
            throw new ValidationException("Total amount must be greater than ".concat(MIN_AMOUNT_VALUE.toString()));
        }
    }

    private void checkCurrentValidation(Event event){
        if(repository.existsByOrderIdAndTransactionId(event.getOrderId(),event.getTransactionId())){
            throw new ValidationException("There's another transaction for this validation");
        }
    }
    private void setEventAmountItems(Event event,Payment payment){
        event.getPayload().setTotalAmount(payment.getTotalAmount());
        event.getPayload().setTotalItems(payment.getTotalItems());
    }
    private void changePaymentToSucess(Payment payment){
        payment.setStatus(EPaymentStatus.SUCESS);
        savePayment(payment);
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

    private void hamdleSuccess(Event event){
        event.setStatus(ESagaStatus.SUCCESS);
        event.setSource(CURRENT_SOURCE);
        addHitory(event,"Payment realized successfully!");
    }

    private void handleFailCurrentNotExecuted(Event event,String message){
        event.setStatus(ESagaStatus.ROLLBACK_PENDING);
        event.setSource(CURRENT_SOURCE);
        addHitory(event,"Fail to realized payment: ".concat(message));
    }

    public void realizeRefound(Event event){
        changePaymentStatusToRefound(event);
        event.setStatus(ESagaStatus.FAIL);
        event.setSource(CURRENT_SOURCE);
        addHitory(event,"Rollback executed for payment");
        producer.sendEvent(jsonUtil.toJson(event));
    }

    private void changePaymentStatusToRefound(Event event){
        var payment = findByOrderIdAndTransactionId(event);
        payment.setStatus(EPaymentStatus.REFUND);
        setEventAmountItems(event,payment);
        savePayment(payment);
    }
}
