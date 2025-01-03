package br.com.microservices.orchestrated.paymentservice.core.dto;

import br.com.microservices.orchestrated.orderservice.core.document.Product;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderProducts {
    private Product product;
    private  int quantity;
}
