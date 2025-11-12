package com.oms.orders.entity;

import com.oms.orders.dto.OrderDto;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    @Id
    private String orderId;
    private List<OrderDto.OrderItem> items;
    private double totalPrice;
    private String status;
    private Instant placedAt;
}
