package com.oms.orders.kafka;

import com.oms.orders.dto.OrderDto;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderProducer {
    private final KafkaTemplate<String, OrderDto> kafkaTemplate;
    private static final String TOPIC = "orders.new";

    public void publishOrder(OrderDto order) {
        kafkaTemplate.send(TOPIC, order);
        System.out.println("Published order " + order.getOrderId() + " to topic " + TOPIC);
    }
}
