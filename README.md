# Purchase Transaction Application

A Spring Boot application for managing purchase transactions with real-time currency conversion capabilities.

## Prerequisites
* Java 17 LTS
* Maven 3.9+

## Technology Stack
- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17 LTS
- **Build**: Maven 3.9+
- **Testing**: JUnit 5, Mockito
- **JSON**: Jackson
- **Logging**: SLF4J
- **HTTP Client**: RestTemplate

## Project Structure
```text
purchase-transaction-app/
├── src/
│   ├── main/
│   │   ├── java/com/purchase/transaction/
│   │   │   ├── controller/      # REST API Controllers
│   │   │   ├── service/         # Business Logic
│   │   │   └── repository/      # Data Access Layer
│   │   └── resources/
│   │       └── application.properties  # Configuration File
│   └── test/
│       └── java/com/purchase/transaction/ # Unit and Integration Tests
├── target/                 # Build Output
├── pom.xml                 # Maven Configuration
└── README.md
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/transactions` | Create new transaction |
| GET | `/api/v1/transactions` | Get all transactions |
| GET | `/api/v1/transactions/{id}` | Get transaction by ID |
| DELETE | `/api/v1/transactions/{id}` | Delete transaction |
| GET | `/api/v1/conversions/currencies/available` | Get available currencies |
| GET | `/api/v1/conversions/{id}?currency=EUR` | Convert transaction to currency |

## Key Features
✅ **Transaction Management**
- Create, read, update, delete transactions
- Persistent file-based storage
- Input validation

✅ **Currency Conversion**
- Convert USD to 19+ currencies
- Real-time exchange rates from US Treasury API
- Cached results for performance

✅ **Error Handling**
- Global exception handler
- Meaningful error messages
- HTTP status codes

✅ **Comprehensive Testing**
- 54 unit tests covering all components
- Service layer mocking
- Integration tests
- 100% test pass rate

✅ **Production Ready**
- Manual constructor injection (no Lombok)
- Robust logging with SLF4J

## Configuration
Edit `src/main/resources/application.properties` to customize:

```properties
server.port=8080                               # Server port
logging.level.root=INFO                        # Root logging level
logging.level.com.purchase.transaction=DEBUG   # App logging level
app.repository.path=./data                     # Data storage location
app.exchange-rate.cache-enabled=true           # Cache exchange rates
```

## Data Storage
Transactions are persistently stored in JSON format at: 
`./data/transactions.json`

To reset data:
```bash
rm ./data/transactions.json
```

## Running Tests
```bash
# Run all unit tests
mvn clean test

# Run specific test class
mvn test -Dtest=PurchaseTransactionServiceTest

# Run with coverage report
mvn test jacoco:report
```

## Quick Test Examples

### Start the Application
```bash
cd purchase-transaction-app
mvn clean package -DskipTests
java -jar target/purchase-transaction-app-1.0.0.jar
```
*The application will start on http://localhost:8080*

### 1. Create a Transaction
```bash
curl -X POST http://localhost:8080/api/v1/transactions \
  -d "description=Laptop%20Purchase&transactionDate=2025-12-01&amount=1250.50"
```

### 2. Get All Transactions
```bash
curl -s http://localhost:8080/api/v1/transactions | jq .
```

### 3. Get Single Transaction
```bash
# Replace {id} with an actual UUID from the previous step
curl -s http://localhost:8080/api/v1/transactions/{id} | jq .
```

### 4. Delete Transaction
```bash
curl -X DELETE http://localhost:8080/api/v1/transactions/{id}
```

## Troubleshooting

**Port 8080 already in use?**
```bash
java -jar target/purchase-transaction-app-1.0.0.jar --server.port=8081
```

**Clear build artifacts:**
```bash
mvn clean
```

**See detailed logs:**
```bash
java -jar target/purchase-transaction-app-1.0.0.jar --logging.level.root=DEBUG
```

## Next Steps
1. **Explore the API**: Use the examples above to test endpoints.
2. **Run the Tests**: Execute `mvn test` to see all test cases.
3. **Modify Data**: Create your own transactions and test the endpoints.
4. **Extend Features**: Add new endpoints or services as needed.
