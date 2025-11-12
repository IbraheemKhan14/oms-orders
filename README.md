# Orders Service

This is the **Orders Service**, a part of the scalable e-commerce microservices at SunKing.  
It handles order placement, order status updates, and integrates with the **Inventory Service** for product stock management.  
Orders are processed asynchronously using **Kafka** for high scalability and resilience.

---

## Overview

- **Orders Service** — Handles order creation, updates, and retrieval.
- **Inventory Service** — Manages product inventory (quantity, pricing).
- **Kafka** — Decouples order submission from processing (used for async order placement).
- **PostgreSQL** — Stores order data (hosted on Supabase or Neon).
- **Feign Client** — Connects Orders Service → Inventory Service.

---  

| Component | Technology |
|------------|-------------|
| Language | Java |
| Framework | Spring Boot 3 |
| Messaging | Apache Kafka |
| Database | MongoDB |
| Build Tool | Maven |
| Deployment | Docker / Render / Railway |
| Communication | REST + OpenFeign |
