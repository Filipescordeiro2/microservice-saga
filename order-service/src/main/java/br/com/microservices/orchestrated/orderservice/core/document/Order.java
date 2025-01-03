package br.com.microservices.orchestrated.orderservice.core.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nonapi.io.github.classgraph.json.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Document(collation = "order")
public class Order {

    @Id
    private String id;
    private String transactionId;
    private List<OrderProducts> products;
    private LocalDateTime createAt;
    private String TransactionId;
    private double totalAmount;
    private int totalItems;

}
