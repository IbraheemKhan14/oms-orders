package com.oms.orders.kafka;

import com.oms.orders.dto.OrderDto;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DLQOrderProducer {
    private final KafkaTemplate<String, OrderDto> kafkaTemplate;
    private static final String DLQ_TOPIC = "orders.failed";

    public void publishFailedOrder(OrderDto order, String reason) {
        System.err.println("Sending order " + order.getOrderId() + " to DLQ due to: " + reason);
        kafkaTemplate.send(DLQ_TOPIC, order);
    }
}
