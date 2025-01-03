package br.com.microservices.orchestrated.orderservice.core.service;

import br.com.microservices.orchestrated.orderservice.core.document.Event;
import br.com.microservices.orchestrated.orderservice.core.document.Order;
import br.com.microservices.orchestrated.orderservice.core.dto.OrderRequest;
import br.com.microservices.orchestrated.orderservice.core.producer.SagaProducer;
import br.com.microservices.orchestrated.orderservice.core.repository.OrderRepository;
import br.com.microservices.orchestrated.orderservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@AllArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final EventService eventService;
    private final SagaProducer sagaProducer;
    private final JsonUtil jsonUtil;

    private static final String TRANSACTION_ID_PATTERN = "%s_%s";

    public Order createOrder(OrderRequest orderRequest) {
        var order = Order.builder()
                .products(orderRequest.getProducts())
                .createAt(LocalDateTime.now())
                .transactionId(
                        String.format(TRANSACTION_ID_PATTERN, Instant.now().toEpochMilli(), UUID.randomUUID().toString())
                )
                .build();

        orderRepository.save(order);
        sagaProducer.sendEvent(jsonUtil.toJson(createPayload(order)));
        return order;
    }

    private Event createPayload(Order order) {
      var event = Event.builder()
              .orderId(order.getId())
              .transactionId(order.getTransactionId())
              .payload(order)
              .createAt(LocalDateTime.now())
              .build();

      eventService.save(event);

      return event;
    }
}
