package com.oms.orders.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "inventoryClient", url = "${inventory.base-url}")
public interface InventoryClient {

    @PostMapping("/reserve")
    Map<String, Object> reserve(@RequestBody Map<String, Object> request);

    @PostMapping("/release")
    Map<String, Object> release(@RequestBody Map<String, Object> request);

    @GetMapping("/products/{productCode}")
    Map<String, Object> getProduct(@PathVariable("productCode") String productCode);
}
