package br.com.microservices.orchestrated.orderservice.core.service;

import br.com.microservices.orchestrated.orderservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.orderservice.core.document.Event;
import br.com.microservices.orchestrated.orderservice.core.dto.EventFilters;
import br.com.microservices.orchestrated.orderservice.core.repository.EventRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.util.ObjectUtils.isEmpty;

@Slf4j
@Service
@AllArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

    public void notifyEnding(Event event){
        event.setOrderId(event.getOrderId());
        event.setCreateAt(LocalDateTime.now());
        save(event);
        log.info("Order {} with saga notified! TransactionId: {}", event.getOrderId(), event.getTransactionId());
    }

    public void save(Event event) {
        eventRepository.save(event);
    }

    public List<Event>findAll(){
        return eventRepository.findAllByOrderByCreateAtDesc();
    }

    private void validateEmptyEvent(EventFilters filters){
            if(isEmpty(filters.getOrderId() ) && isEmpty(filters.getTransactionId())){
                throw new ValidationException("OrderId or TransactionId must be informed");
            }
    }

    private Event findByOrderId(String orderId){
        return eventRepository
                .findTop1ByOrderIdOrderByCreateAtDesc(orderId)
                .orElseThrow(() -> new ValidationException("Event not found by orderId."));
    }

    private Event findByTransactionId(String transactionId){
        return eventRepository
                .findTop1ByTransactionIdOrderByCreateAtDesc(transactionId)
                .orElseThrow(() -> new ValidationException("Event not found by transactionId."));
    }

    public Event findByFilters(EventFilters filters){
        validateEmptyEvent(filters);
        if(!isEmpty(filters.getOrderId())){
            return findByOrderId(filters.getOrderId());
        }
        else{
            return findByTransactionId(filters.getTransactionId());
        }
    }
}
