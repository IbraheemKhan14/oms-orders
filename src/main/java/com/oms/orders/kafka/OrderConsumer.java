package com.oms.orders.kafka;

import com.oms.orders.dto.OrderDto;
import com.oms.orders.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class OrderConsumer {

    private final OrderService orderService;
    private final DLQOrderProducer dlqOrderProducer;

    @KafkaListener(topics = "orders.new", groupId = "orders-group")
    public void consumeOrder(ConsumerRecord<String, OrderDto> record, Acknowledgment ack, Consumer<?, ?> consumer) {
        OrderDto request = record.value();
        System.out.println("Consumed order: " + request.getOrderId());
        try {
            orderService.processOrder(request);
            consumer.commitSync(Collections.singletonMap(
                    new TopicPartition(record.topic(), record.partition()),
                    new OffsetAndMetadata(record.offset() + 1)));
        } catch (Exception ex) {
            dlqOrderProducer.publishFailedOrder(request, ex.getMessage());
        }
    }
}