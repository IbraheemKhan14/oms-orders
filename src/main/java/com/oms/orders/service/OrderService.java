package com.oms.orders.service;

import com.oms.orders.client.InventoryClient;
import com.oms.orders.dto.OrderDto;
import com.oms.orders.entity.Order;
import com.oms.orders.enums.OrderStatus;
import com.oms.orders.repository.OrderRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(
            OrderStatus.PENDING, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.FAILED, OrderStatus.CANCELED),
            OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.SHIPPED, OrderStatus.CANCELED),
            OrderStatus.SHIPPED, EnumSet.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED, EnumSet.noneOf(OrderStatus.class),
            OrderStatus.FAILED, EnumSet.noneOf(OrderStatus.class),
            OrderStatus.CANCELED, EnumSet.noneOf(OrderStatus.class)
    );

    public void processOrder(OrderDto req) {
        double total = 0.0;
        boolean success = true;
        List<OrderDto.OrderItem> reservedItems = new ArrayList<>();

        for (OrderDto.OrderItem item : req.getItems()) {
            try {
                Map<String, Object> productResponse = inventoryClient.getProduct(item.getProductCode());
                Map<String, Object> data = (Map<String, Object>) productResponse.get("data");

                if (data == null || !data.containsKey("price")) {
                    throw new IllegalStateException("Product not found: " + item.getProductCode());
                }

                double price = Double.parseDouble(data.get("price").toString());
                item.setPrice(price);
                total += price * item.getQuantity();

                Map<String, Object> reserveReq = new HashMap<>();
                reserveReq.put("productCode", item.getProductCode());
                reserveReq.put("quantity", item.getQuantity());

                inventoryClient.reserve(reserveReq);

                reservedItems.add(item);
            } catch (FeignException.Conflict ex) {
                System.err.println("Out of stock for " + item.getProductCode());
                success = false;
                break;
            } catch (Exception ex) {
                System.err.println("Error while reserving " + item.getProductCode() + ": " + ex.getMessage());
                success = false;
                break;
            }
        }

        if (!success) {
            // we need to rollback previously reserved items, even if one fails one need to cancel that order
            for (OrderDto.OrderItem item : reservedItems) {
                try {
                    Map<String, Object> releaseReq = Map.of(
                            "productCode", item.getProductCode(),
                            "quantity", item.getQuantity());
                    inventoryClient.release(releaseReq);
                    System.out.println("Rolled back reservation for " + item.getProductCode());
                } catch (Exception e) {
                    System.err.println("Failed to release " + item.getProductCode());
                }
            }
        }

        Order order = Order.builder()
                .orderId(req.getOrderId())
                .items(req.getItems())
                .totalPrice(total)
                .status(success ? "CONFIRMED" : "FAILED")
                .placedAt(Instant.now())
                .build();

        orderRepository.save(order);

        if (!success) {
            throw new IllegalStateException("Order placement failed. Stock not available.");
        }

        System.out.println("Order " + req.getOrderId() + " saved as " + order.getStatus());
    }

    private void fallbackOrderProcessing(OrderDto req, Throwable ex) {
        Order failed = Order.builder()
                .orderId(req.getOrderId())
                .items(req.getItems())
                .totalPrice(0)
                .status("FAILED")
                .placedAt(Instant.now())
                .build();
        orderRepository.save(failed);
        System.err.println("Fallback triggered for " + req.getOrderId() + ": " + ex.getMessage());
    }

    public Order updateStatus(String orderId, String newStatusRaw) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order " + orderId + " not found"));

        OrderStatus current = parse(order.getStatus());
        OrderStatus next = parse(newStatusRaw);

        if (!ALLOWED.getOrDefault(current, Set.of()).contains(next)) {
            throw new IllegalStateException("Cannot transition from " + current + " to " + next);
        }

        order.setStatus(next.name());
        return orderRepository.save(order);
    }

    private OrderStatus parse(String raw) {
        try {
            return OrderStatus.valueOf(raw.trim().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Unsupported status: " + raw
                    + ". Allowed values: " + Arrays.toString(OrderStatus.values()));
        }
    }
}
