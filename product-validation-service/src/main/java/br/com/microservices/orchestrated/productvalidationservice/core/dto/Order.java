package br.com.microservices.orchestrated.productvalidationservice.core.dto;

import br.com.microservices.orchestrated.orchestratorservice.core.dto.OrderProducts;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class Order {

    private String id;
    private List<OrderProducts> products;
    private LocalDateTime createAt;
    private String TransactionId;
    private double totalAmount;
    private int totalItems;

}
