# CLP Product Connector

## Overview
The CLP Product Connector is a Spring Boot-based microservice that implements the Event-Carried State Transfer (ECST) pattern for managing product data synchronization between systems. It provides functionality for product data management, status tracking, and event-based communication using RabbitMQ.

## Architecture

### Key Components

1. **Event-Carried State Transfer (ECST)**
   - Enables loosely coupled communication between systems
   - One system publishes events containing complete state change data
   - Receivers can process and update local data copies independently
   - Improves system resilience and scalability

2. **Core Services**
   - `ProductReadModelService`: Handles product data queries and updates
   - `ProductStatusService`: Manages product status lifecycle
   - `ProductEventRabbitListener`: Processes product change events
   - `ProductClient`: Feign client for interacting with the BaseProduct controller

3. **Data Model**
   - `ProductReadModelEntity`: Main product data representation
   - `ProductEventEntity`: Event data storage
   - `ProductStatusEntity`: Product status tracking

### Event Flow
1. Frontend (FE) initiates product updates via REST API
2. System processes changes and publishes events to RabbitMQ
3. Subscribers receive and process events asynchronously
4. Status updates are tracked and synchronized

## Configuration

### RabbitMQ Configuration
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
                host: sbalt0011969.t.t.bzwbk
                port: 4443
                virtual-host: local-dev
                username: admin
                password: admin

      bindings:
        productChangedEventConsumer-in-0:
          destination: ${productconnector.exchange.name:CLP_PRODUCT_CHANGED_EVENT_FANOUT_DEV}
          group: ${productconnector.queue.name:default}
          content-type: application/json
```

### Event Processing
- Max retry attempts: 5
- Initial backoff: 5000ms
- Max backoff: 60000ms
- Backoff multiplier: 2

## API Endpoints

### Product Search API
```
POST /product-connector/products/search
```
Search for BaseProductDataDto using provided criteria:
- Required parameters (at least one): 
  - productIds
  - customerIds
  - pricingOrderId
  - launchOrderId
- Optional filters:
  - stages
  - types

### Product Synchronization
```
POST /product-connector/products/synchronize
```
Synchronize products for given customer IDs

## Event Handling

The system implements a robust event handling mechanism with:
- Automatic retry logic
- Event persistence
- Status tracking
- Error handling with specific exceptions

### Status States
- SYNCHRONIZED
- SUSPENDED

## Error Handling

1. **Exception Types**
   - `ProductSynchronizationException`
   - `ProcessingProductChangedEventException`

2. **Error Recovery**
   - Automatic retry mechanism for failed events
   - Event suspension for unrecoverable errors
   - Detailed error logging

## Development Guidelines

1. **Event Processing**
   - Always handle events idempotently
   - Implement proper error handling
   - Use appropriate logging levels

2. **Database Operations**
   - Use transactions appropriately (@Transactional)
   - Handle potential race conditions
   - Implement proper indexing for queries

3. **Testing**
   - Unit tests for services
   - Integration tests for event processing
   - Test error scenarios and recovery

## Dependencies

- Spring Boot
- Spring Cloud Stream
- RabbitMQ
- JPA/Hibernate
- Feign Client
- Lombok
- Jakarta Persistence

## Building and Deployment

### Prerequisites
- Java 11 or higher
- Maven
- RabbitMQ instance

### Build Commands
```bash
mvn clean install
```

### Deployment Configuration
Ensure proper configuration of:
- Database connection
- RabbitMQ connection
- Service endpoints
- Environment-specific variables

## Monitoring and Maintenance

### Health Checks
- Database connectivity
- RabbitMQ connection
- Event processing status

### Logging
- Application logs with appropriate levels
- Event processing logs
- Error tracking and monitoring

## Security

- Protected access levels for different components
- Secure communication with external services
- Proper authentication and authorization

## Performance Considerations

1. **Database**
   - Proper indexing for search queries
   - Batch processing for bulk operations
   - Connection pool optimization

2. **Event Processing**
   - Configurable batch sizes
   - Retry mechanisms with exponential backoff
   - Resource utilization monitoring

3. **API Performance**
   - Response pagination
   - Caching where appropriate
   - Query optimization

## Troubleshooting

Common issues and solutions:

1. **Event Processing Issues**
   - Check RabbitMQ connection
   - Verify event format
   - Check for suspended updates

2. **Database Issues**
   - Verify connection settings
   - Check for deadlocks
   - Monitor performance metrics

3. **API Issues**
   - Validate request format
   - Check authentication
   - Verify service availability
