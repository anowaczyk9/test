# CLP Product Connector Library

## Overview
CLP Product Connector is a library that enables product data synchronization between COP-backend and microservices. It implements Event-Carried State Transfer (ECST) pattern to maintain consistency of product data across distributed systems.

## Integration Guide

### Database Setup
The library creates and manages its own tables in your application's database. Required tables:
- PRODUCT_EVENT
- PRODUCT_STATUS
- PRODUCT_READ_MODEL

### Dependencies
Add to your `pom.xml`:
```xml
<dependency>
    <groupId>pl.santander.clp</groupId>
    <artifactId>clp-product-connector</artifactId>
    <version>${clp-product-connector.version}</version>
</dependency>
```

### Configuration

1. **Required Properties**
```yaml
productconnector:
  exchange:
    name: YOUR_EXCHANGE_NAME  # Required: Name of the exchange for product events
  queue:
    name: YOUR_QUEUE_NAME    # Required: Name of the queue for consuming events
```

2. **RabbitMQ Configuration**
```yaml
spring:
  cloud:
    stream:
      binders:
        local_dev:
          type: rabbit
          environment:
            spring:
              rabbitmq:
                host: ${your-rabbitmq-host}
                port: ${your-rabbitmq-port}
                virtual-host: ${your-virtual-host}
                username: ${your-username}
                password: ${your-password}
```

2. **Event Consumer Configuration**
```yaml
bindings:
  productChangedEventConsumer-in-0:
    destination: ${productconnector.exchange.name:CLP_PRODUCT_CHANGED_EVENT_FANOUT_DEV}
    group: ${productconnector.queue.name:default}
    content-type: application/json
```

## Usage

### Initialize in Your Microservice
```java
@Import({ProductConnectorConfiguration.class})
public class YourServiceConfiguration {
    // Your configuration
}
```

### Liquibase Setup
1. Include library's changelog in your application:
```yaml
spring:
  liquibase:
    change-log: classpath:db/changelog/db.productconnector-changelog-master.yaml
```

2. Ensure your application's database user has necessary privileges for table creation and modification.

### Product Synchronization API
```java
@Autowired
private ProductReadModelApi productReadModelApi;

// Synchronize products for specific customers
void syncProducts(Set<Long> customerIds) {
    productReadModelApi.synchronizeProducts(customerIds);
}

// Search products
List<BaseProductDataDto> findProducts(ProductSearchCriteria criteria) {
    return productReadModelApi.findByCriteria(criteria);
}
```

## Event Flow
1. COP-backend publishes product changes
2. Library receives events via RabbitMQ
3. Microservice gets notified through event listeners
4. Product data is synchronized automatically

## Key Features
- Automatic product data synchronization
- Event-based architecture
- Configurable retry mechanisms
- Status tracking
- Error handling with automatic recovery

## Error Handling
- Automatic retries for failed synchronizations
- Configurable backoff strategy
- Event suspension for unrecoverable errors

## Monitoring
Monitor synchronization status through:
- `/actuator/health` endpoint
- Logging with correlation IDs
- RabbitMQ queue metrics

## Best Practices
1. Configure appropriate retry policies
2. Monitor queue depth
3. Handle suspended updates
4. Implement proper error handling
5. Use provided APIs for data access