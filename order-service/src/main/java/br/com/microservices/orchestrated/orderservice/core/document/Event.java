package br.com.microservices.orchestrated.orderservice.core.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Event {

    private String id;
    private String trasactionId;
    private String orderId;
    private Order payload;
    private String source;
    private String status;
    private List<History>eventHistory;
    private LocalDateTime createAt;

}
