package com.oms.orders.controller;

import com.oms.orders.common.utils.ApiErrorResponse;
import com.oms.orders.common.utils.ApiResponse;
import com.oms.orders.dto.OrderDto;
import com.oms.orders.entity.Order;
import com.oms.orders.kafka.OrderProducer;
import com.oms.orders.repository.OrderRepository;
import com.oms.orders.service.OrderService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderProducer producer;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public OrderController(OrderProducer producer, OrderRepository orderRepository, OrderService orderService) {
        this.producer = producer;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    @RateLimiter(name = "orderPlacementLimiter")
    @PostMapping
    public ResponseEntity<?> placeOrder(@RequestBody OrderDto req) {
        try {
            req.setOrderId(UUID.randomUUID().toString());
            producer.publishOrder(req);
            return ResponseEntity.accepted().body(
                    ApiResponse.ok(String.format(
                            "Order %s queued for processing at %s",
                            req.getOrderId(), Instant.now()))
            );
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiErrorResponse.of("ORDER_FAILED", ex.getMessage()));
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable String orderId) {
        Optional<Order> order = orderRepository.findById(orderId);
        return order.<ResponseEntity<?>>map(o -> ResponseEntity.ok(ApiResponse.ok(o)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiErrorResponse.of("ORDER_NOT_FOUND", "No order found with ID " + orderId)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable String id,
                                          @RequestBody Map<String, String> body) {
        try {
            String newStatus = body.getOrDefault("status", "PENDING");
            Order updated = orderService.updateStatus(id, newStatus);
            return ResponseEntity.ok(ApiResponse.ok(updated));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiErrorResponse.of("INVALID_STATUS", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiErrorResponse.of("INVALID_TRANSITION", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.of("ORDER_NOT_FOUND", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiErrorResponse.of("STATUS_UPDATE_FAILED", ex.getMessage()));
        }
    }
}
